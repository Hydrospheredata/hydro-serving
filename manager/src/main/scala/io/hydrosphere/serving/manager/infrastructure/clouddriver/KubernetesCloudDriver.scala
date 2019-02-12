package io.hydrosphere.serving.manager.infrastructure.clouddriver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import cats.effect.{Async, Sync}
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.option._
import io.hydrosphere.serving.manager.config.{CloudDriverConfiguration, DockerRepositoryConfiguration, SidecarConfig}
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
import scala.util.Try

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
    } yield ()
  }
  
  override def serviceList(): F[Seq[CloudService]] = AsyncUtil.futureAsync {
    k8s.listInNamespace[ServiceList](conf.kubeNamespace).map(services => {
      
      val cloudServices = services
        .filter(svc => svc.metadata.labels.contains("hs_service_marker"))
        .map(kubeServiceToCloudService)
      
      cloudServices ++ DockerUtil.createFakeHttpServices(cloudServices)
    })
  }

  override def deployService(service: Servable, modelVersion: DockerImage, host: Option[HostSelector]): F[CloudService] = AsyncUtil.futureAsync {
    import LabelSelector.dsl._
  
    val dockerRepoHost = dockerRepoConf.pullHost.getOrElse(dockerRepoConf.host)
    
    //TODO!
    val hackyImage = modelVersion.replaceUser(dockerRepoHost).toTry.get
    
    val template = Pod.Template.Spec(
      metadata = ObjectMeta(name = service.serviceName),
      spec = Some(Pod.Spec()
        .addImagePullSecretRef(conf.kubeRegistrySecretName)
        .addVolume(Volume("shared-model", Volume.EmptyDir()))
      ))
      .addContainer(Container("runtime", hackyImage.fullName).exposePort(9090))
      .addLabel("app" -> service.serviceName)

    val deployment = apps.v1.Deployment(metadata = ObjectMeta(name = service.serviceName, namespace = conf.kubeNamespace))
      .withReplicas(1)
      .withTemplate(template)
      .withLabelSelector("app" is service.serviceName)

    val kubeService = skuber.Service(metadata = ObjectMeta(name = service.serviceName, namespace = conf.kubeNamespace))
      .withSelector("app" -> service.serviceName)
      .exposeOnPort(skuber.Service.Port("grpc", Protocol.TCP, 9090))
      .addLabels(DefaultConstants.getModelLabels(service) ++ Map("service_name" -> service.serviceName))

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
      svcList <- namespacedContext.listSelected[ServiceList]("SERVICE_ID" is serviceId.toString)
      svc <- Future {
        //TODO ???
        svcList.headOption.getOrElse(throw new RuntimeException(s"kube service with id$serviceId not found"))
      }
      _ <- namespacedContext.delete[skuber.Service](svc.metadata.name)
      _ <- namespacedContext.delete[apps.v1.Deployment](svc.metadata.name)
    } yield
      cloudServiceBus.removed(kubeServiceToCloudService(svc))
  }
  
  //TODO
  private def serviceIdFromMeta(meta: skuber.ObjectMeta): Option[Long] = {
    val maybeId = meta.labels.get("service_id") orElse meta.labels.get("SERVICE_ID")
    maybeId.map(s => if (s.startsWith("id")) s.replaceFirst("id", "") else s)
      .flatMap(s => Try(s.toLong).toOption)
  }

  private def kubeServiceToCloudService(svc: skuber.Service): CloudService = {
    val meta = svc.metadata
    
    val instanceId = meta.uid
    
    val serviceId = serviceIdFromMeta(meta).getOrElse(0L)
    val serviceName = DefaultConstants.specialNamesByIds.getOrElse(serviceId, meta.labels.getOrElse("service_name", ""))
    
    val ip = svc.spec.fold("")(_.clusterIP)
    val port = svc.spec.flatMap(_.ports.find(_.name == "grpc")).fold(9091)(_.port)
    
    val modelInstance = meta.labels.get("deployment_type") match {
      case Some("model") => ModelInstance(instanceId).some
      case _ => None
    }
    
    CloudService(
      id = serviceId,
      serviceName = serviceName,
      statusText = "",
      cloudDriverId = svc.metadata.uid,
      instances = Seq(
        ServiceInstance(
          advertisedHost = ip,
          advertisedPort = port,
          instanceId = instanceId,
          mainApplication = MainApplicationInstance(
            instanceId = instanceId,
            host = ip,
            port = port
          ),
          model = modelInstance
        ))
    )
  }
}