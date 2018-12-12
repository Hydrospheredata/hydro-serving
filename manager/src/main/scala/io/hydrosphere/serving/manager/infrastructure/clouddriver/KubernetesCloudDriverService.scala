package io.hydrosphere.serving.manager.infrastructure.clouddriver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import io.hydrosphere.serving.manager.config.{CloudDriverConfiguration, DockerRepositoryConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.domain.clouddriver.DefaultConstants._
import io.hydrosphere.serving.manager.domain.clouddriver._
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.service.Service
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.InternalManagerEventsPublisher
import org.apache.logging.log4j.scala.Logging
import skuber._
import skuber.api.client.{EventType, RequestContext}
import skuber.json.format._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls

class KubernetesCloudDriverService(managerConfiguration: ManagerConfiguration, internalManagerEventsPublisher: InternalManagerEventsPublisher)(implicit val ex: ExecutionContext, actorSystem: ActorSystem) extends CloudDriverAlgebra[Future] with Logging {

  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  val conf: CloudDriverConfiguration.Kubernetes = managerConfiguration.cloudDriver.asInstanceOf[CloudDriverConfiguration.Kubernetes]
  val k8s: RequestContext = k8sInit(K8SConfiguration.useProxyAt(s"http://${conf.proxyHost}:${conf.proxyPort}"))

  {
    val monitor = Sink.foreach[K8SWatchEvent[skuber.Service]] { serviceEvent: K8SWatchEvent[skuber.Service] =>
      if (serviceEvent._object.metadata.namespace == conf.kubeNamespace) {
        serviceEvent._type match {
          case EventType.ADDED =>
            internalManagerEventsPublisher.cloudServiceDetected(Seq(kubeServiceToCloudService(serviceEvent._object)))
            logger.debug(s"service ${serviceEvent._object.name} added")
          case EventType.DELETED =>
            internalManagerEventsPublisher.cloudServiceRemoved(Seq(kubeServiceToCloudService(serviceEvent._object)))
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

  override def getMetricServiceTargets: Future[Seq[MetricServiceTargets]] = Future {
    Seq[MetricServiceTargets]()
  }

  override def serviceList(): Future[Seq[CloudService]] = {
    for {
      podsList: PodList <- k8s.listInNamespace[PodList](conf.kubeNamespace)
      serviceList: ServiceList <- k8s.listInNamespace[ServiceList](conf.kubeNamespace)
      cloudServices = serviceList
        .filter(svc => svc.metadata.labels.contains("hs_service_marker"))
        .map({ service =>
          val pods = podsList.filter(pod => service.spec.map(_.selector.toSet).getOrElse(Set.empty[(String, String)]).subsetOf(pod.metadata.labels.toSet))
          val image = for {
            pod <- pods.headOption
            spec <- pod.spec
            container <- spec.containers.headOption
          } yield container.image
          kubeServiceToCloudService(service, image.getOrElse(":"))
        })
    } yield cloudServices ++ createFakeHttpServices(cloudServices)
  }

  override def deployService(service: Service, runtime: DockerImage, modelVersion: DockerImage, host: Option[HostSelector]): Future[CloudService] = {
    import LabelSelector.dsl._

    val runtimeContainer = Container("runtime", runtime.fullName)
      .exposePort(9090)
      .mount("shared-model", "/model")

    val dockerRepoConf = managerConfiguration.dockerRepository.asInstanceOf[DockerRepositoryConfiguration.Remote]
    val dockerRepoHost = dockerRepoConf.pullHost match {
      case Some(host) => host
      case None => dockerRepoConf.host
    }

    val modelContainer = Container("model", s"$dockerRepoHost/${modelVersion.fullName}")
      .mount("shared-model", "/shared/model")
      .withEntrypoint("cp")
      .withArgs("-a", "/model/.", "/shared/model/")

    val template = Pod.Template.Spec(metadata = ObjectMeta(name = service.serviceName), spec = Some(Pod.Spec().addImagePullSecretRef(conf.kubeRegistrySecretName).addVolume(Volume("shared-model", Volume.EmptyDir()))))
      .addInitContainer(modelContainer)
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

    (for {
      svc <- namespacedContext.create(kubeService)
      _ <- namespacedContext.create(deployment)
    } yield kubeServiceToCloudService(svc)).map(cloudService => {
      internalManagerEventsPublisher.cloudServiceDetected(Seq(cloudService))
      cloudService
    })
  }

  override def services(serviceIds: Set[Long]): Future[Seq[CloudService]] = serviceList().map(_.filter(cs => serviceIds.contains(cs.id)))

  override def removeService(serviceId: Long): Future[Unit] = {
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
      internalManagerEventsPublisher.cloudServiceRemoved(Seq(kubeServiceToCloudService(svc)))
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
          host = svc.spec.map(_.clusterIP).getOrElse(""),
          port = svc.spec.flatMap(_.ports.headOption).map(_.port).getOrElse(9091)
        ),
        sidecar = SidecarInstance(
          instanceId = svc.metadata.uid,
          host = svc.spec.map(_.clusterIP).getOrElse(""),
          ingressPort = svc.spec.flatMap(_.ports.headOption).map(_.port).getOrElse(9091),
          egressPort = svc.spec.flatMap(_.ports.headOption).map(_.port).getOrElse(9091),
          adminPort = svc.spec.flatMap(_.ports.headOption).map(_.port).getOrElse(9091)
        ),
        model = if (svc.metadata.labels.getOrElse("deployment_type", "") == "model") Some(ModelInstance(svc.metadata.uid)) else None,
        advertisedHost = svc.spec.map(_.clusterIP).getOrElse(""),
        advertisedPort = svc.spec.flatMap(_.ports.find(_.name == "grpc")).map(_.port).getOrElse(9091)
      ))
    )
  }
}