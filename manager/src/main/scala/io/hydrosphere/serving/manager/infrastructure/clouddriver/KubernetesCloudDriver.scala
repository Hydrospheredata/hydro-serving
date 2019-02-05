package io.hydrosphere.serving.manager.infrastructure.clouddriver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import cats.effect.Async
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.hydrosphere.serving.manager.config.{CloudDriverConfiguration, DockerRepositoryConfiguration}
import io.hydrosphere.serving.manager.domain.clouddriver._
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.servable.Servable
import io.hydrosphere.serving.manager.infrastructure.clouddriver.docker.DockerUtil
import io.hydrosphere.serving.manager.infrastructure.envoy.events.CloudServiceDiscoveryEventBus
import io.hydrosphere.serving.manager.util.AsyncUtil
import org.apache.logging.log4j.scala.Logging
import skuber._
import skuber.api.client.{EventType, RequestContext}
import skuber.json.format._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls

class KubernetesCloudDriver[F[_]: Async](
  conf: CloudDriverConfiguration.Kubernetes,
  dockerRepoConf: DockerRepositoryConfiguration.Remote,
  cloudServiceBus: CloudServiceDiscoveryEventBus[F]
)(implicit val ex: ExecutionContext,
  actorSystem: ActorSystem
) extends CloudDriver[F] with Logging {

  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  val k8s: RequestContext = k8sInit(K8SConfiguration.useProxyAt(s"http://${conf.proxyHost}:${conf.proxyPort}"))

  {
    val monitor = Sink.foreach[K8SWatchEvent[skuber.Service]] { serviceEvent: K8SWatchEvent[skuber.Service] =>
      if (serviceEvent._object.metadata.namespace == conf.kubeNamespace) {
        serviceEvent._type match {
          case EventType.ADDED =>
            cloudServiceBus.detected(kubeServiceToCloudService(serviceEvent._object))
            logger.debug(s"service ${serviceEvent._object.name} added")
          case EventType.DELETED =>
            cloudServiceBus.removed(kubeServiceToCloudService(serviceEvent._object))
            logger.debug(s"service ${serviceEvent._object.name} deleted")
          case EventType.MODIFIED => logger.debug(s"service ${serviceEvent._object.name} modified")
          case EventType.ERROR => logger.debug(s"service ${serviceEvent._object.name} error")
        }
      }
    }

    for {
      serviceWatch <- k8s.watchAll[skuber.Service]()
      _ <- serviceWatch.runWith(monitor)
    } yield Unit
  }

  override def serviceList(): F[Seq[CloudService]] = AsyncUtil.futureAsync {
    for {
      podsList: PodList <- k8s.listInNamespace[PodList](conf.kubeNamespace)
      serviceList: ServiceList <- k8s.listInNamespace[ServiceList](conf.kubeNamespace)
      cloudServices = serviceList
        .filter(svc => svc.metadata.labels.contains("hs_service_marker"))
        .map({ service =>
          val pods = podsList.filter(pod => service.spec.fold(Set.empty[(String, String)])(_.selector.toSet).subsetOf(pod.metadata.labels.toSet))
          val image = for {
            pod <- pods.headOption
            spec <- pod.spec
            container <- spec.containers.headOption
          } yield container.image
          kubeServiceToCloudService(service, image.getOrElse(":"))
        })
    } yield cloudServices ++ DockerUtil.createFakeHttpServices(cloudServices)
  }

  override def deployService(service: Servable, modelVersion: DockerImage, host: Option[HostSelector]): F[CloudService] = AsyncUtil.futureAsync {
    import LabelSelector.dsl._

    val runtimeContainer = Container("runtime", modelVersion.fullName)
      .exposePort(9090)
    //      .mount("shared-model", "/model")

    val dockerRepoHost = dockerRepoConf.pullHost match {
      case Some(host) => host
      case None => dockerRepoConf.host
    }

    //    val modelContainer = Container("model", s"$dockerRepoHost/${modelVersion.fullName}")
    //      .mount("shared-model", "/shared/model")
    //      .withEntrypoint("cp")
    //      .withArgs("-a", "/model/.", "/shared/model/")

    val template = Pod.Template.Spec(
      metadata = ObjectMeta(name = service.serviceName),
      spec = Some(Pod.Spec()
        .addImagePullSecretRef(conf.kubeRegistrySecretName)
        .addVolume(Volume("shared-model", Volume.EmptyDir()))
      )
    )
      //      .addInitContainer(modelContainer)
      .addContainer(runtimeContainer)
      .addLabel("app" -> service.serviceName)

    val deployment = apps.v1.Deployment(metadata = ObjectMeta(name = service.serviceName, namespace = conf.kubeNamespace))
      .withReplicas(1)
      .withTemplate(template)
      .withLabelSelector("app" is service.serviceName)

    val kubeService = skuber.Service(metadata = ObjectMeta(name = service.serviceName, namespace = conf.kubeNamespace))
      .withSelector("app" -> service.serviceName)
      .exposeOnPort(skuber.Service.Port("grpc", Protocol.TCP, 9090))
      .addLabels(DefaultConstants.getModelLabels(service))

    val namespacedContext = k8s.usingNamespace(conf.kubeNamespace)

    for {
      svc <- namespacedContext.create(kubeService)
      _ <- namespacedContext.create(deployment)
    } yield kubeServiceToCloudService(svc)
  }.flatMap { cloudService =>
    cloudServiceBus.detected(cloudService).as(cloudService)
  }

  override def removeService(serviceId: Long): F[Unit] = AsyncUtil.futureAsync {
    import LabelSelector.dsl._
    val namespacedContext = k8s.usingNamespace(conf.kubeNamespace)
    for {
      svcList <- namespacedContext.listSelected[ServiceList]("service_id" is s"id$serviceId")
      svc <- Future {
        svcList.headOption.getOrElse(throw new RuntimeException(s"kube service with id$serviceId not found"))
      }
      _ <- namespacedContext.delete[skuber.Service](svc.metadata.name)
      _ <- namespacedContext.delete[apps.v1.Deployment](svc.metadata.name)
    } yield
      cloudServiceBus.removed(kubeServiceToCloudService(svc))
  }

  private def kubeServiceToCloudService(svc: skuber.Service, image: String = ""): CloudService = {
    val Array(imageName, imageVersion, _*) = image.split(":") ++ Array.fill(2)("")
    val serviceId = svc.metadata.labels.getOrElse("service_id", "id0").replaceFirst("id", "").toLong
    CloudService(
      id = serviceId,
      serviceName = DefaultConstants.specialNamesByIds.getOrElse(serviceId, svc.metadata.labels.getOrElse("service_name", "")),
      statusText = "",
      cloudDriverId = svc.metadata.uid,
      image = DockerImage(
        name = imageName,
        tag = imageVersion
      ),
      instances = Seq(ServiceInstance(
        instanceId = svc.metadata.uid,
        mainApplication = MainApplicationInstance(
          instanceId = svc.metadata.uid,
          host = svc.spec.fold("")(_.clusterIP),
          port = svc.spec.flatMap(_.ports.headOption).fold(9091)(_.port)
        ),
        sidecar = SidecarInstance(
          instanceId = svc.metadata.uid,
          host = svc.spec.fold("")(_.clusterIP),
          ingressPort = svc.spec.flatMap(_.ports.headOption).fold(9091)(_.port),
          egressPort = svc.spec.flatMap(_.ports.headOption).fold(9091)(_.port),
          adminPort = svc.spec.flatMap(_.ports.headOption).fold(9091)(_.port)
        ),
        model = if (svc.metadata.labels.getOrElse("deployment_type", "") == "model") Some(ModelInstance(svc.metadata.uid)) else None,
        advertisedHost = svc.spec.fold("")(_.clusterIP),
        advertisedPort = svc.spec.flatMap(_.ports.find(_.name == "grpc")).fold(9091)(_.port)
      ))
    )
  }
}