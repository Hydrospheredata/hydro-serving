package io.hydrosphere.serving.manager.service.clouddriver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import io.hydrosphere.serving.manager.{KubernetesCloudDriverConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.Service
import io.hydrosphere.serving.manager.service.internal_events.InternalManagerEventsPublisher
import io.hydrosphere.serving.manager.service.clouddriver.CloudDriverService._
import org.apache.logging.log4j.scala.Logging
import skuber._
import skuber.api.client.{EventType, RequestContext}
import skuber.json.format._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls
import scala.util.Try

class KubernetesCloudDriverService(managerConfiguration: ManagerConfiguration, internalManagerEventsPublisher: InternalManagerEventsPublisher)(implicit val ex: ExecutionContext, actorSystem: ActorSystem) extends CloudDriverService with Logging {
  
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()

  val conf: KubernetesCloudDriverConfiguration = managerConfiguration.cloudDriver.asInstanceOf[KubernetesCloudDriverConfiguration]
  val k8s: RequestContext = k8sInit(K8SConfiguration.useProxyAt(s"http://${conf.proxyHost}:${conf.proxyPort}"))

//  {
//    val monitor = Sink.foreach[K8SWatchEvent[skuber.Service]] { serviceEvent: K8SWatchEvent[skuber.Service] =>
//      if (serviceEvent._object.metadata.namespace == "serving") {
//        serviceEvent._type match {
//          case EventType.ADDED =>
//            internalManagerEventsPublisher.cloudServiceDetected(Seq(CloudService(
//              id = serviceEvent._object.metadata.labels.getOrElse("service_id", "id0").replace("id", "").toLong,
//              serviceName = serviceEvent._object.metadata.labels.getOrElse("service_name", ""),
//              statusText = serviceEvent._type.toString,
//              cloudDriverId = "",
//              environmentName = None,
//              runtimeInfo = MainApplicationInstanceInfo(
//                runtimeId = 
//              )
//            )))
//            println(s"service ${serviceEvent._object.name} added")
//          case EventType.DELETED =>
//            internalManagerEventsPublisher.cloudServiceRemoved(toRemove)
//            println(s"service ${serviceEvent._object.name} deleted")
//          case EventType.MODIFIED => println(s"service ${serviceEvent._object.name} modified")
//          case EventType.ERROR => println(s"service ${serviceEvent._object.name} error")
//        }
//      }
//    }
//
//    for {
//      serviceWatch <- k8s.watchAll[skuber.Service]()
//      _ <- serviceWatch.runWith(monitor)
//    } yield Unit
//  }

  override def getMetricServiceTargets(): Future[Seq[MetricServiceTargets]] = Future{ Seq[MetricServiceTargets]() }

  override def serviceList(): Future[Seq[CloudService]] = {
    for {
      podsList: PodList <- k8s.listInNamespace[PodList]("serving")
      serviceList: ServiceList <- k8s.listInNamespace[ServiceList]("serving")
      cloudServices = serviceList
        .filter(svc => svc.metadata.labels.contains("hs_service_marker") /*&& svc.metadata.labels.getOrElse("deployment_type", "") == "model"*/)
        .map({ service =>
          val sidecars = serviceList
            .filter(_.metadata.labels.getOrElse("deployment_type", "") == "sidecar")
            .map(service => {
              SidecarInstance(
                instanceId = service.metadata.uid,
                host = service.spec.map(_.clusterIP).getOrElse(""),
                ingressPort = 8080,
                egressPort = 8081,
                adminPort = 8082
              )
            })
          val pods = podsList.filter(pod => service.spec.map(_.selector.toSet).getOrElse(Set.empty[(String, String)]).subsetOf(pod.metadata.labels.toSet))
          val serviceId = service.metadata.labels.getOrElse("service_id", "id0").replaceFirst("id", "").toLong
          val image = for {
            pod <- pods.headOption
            spec <- pod.spec
            container <- spec.containers.headOption
          } yield container.image
          val Array(imageName, imageVersion, _*) = image.getOrElse("").split(":") ++ Array.fill(2)("")
          CloudService(
            id = serviceId,
            serviceName = specialNamesByIds.getOrElse(serviceId, service.metadata.labels.getOrElse("service_name", "")),
            statusText = pods.headOption.flatMap(_.status).flatMap(_.phase).map(_.toString).getOrElse(""),
            cloudDriverId = service.metadata.uid,
            environmentName = service.metadata.labels.get("environment"),
            runtimeInfo = MainApplicationInstanceInfo(
              runtimeId = service.metadata.labels.getOrElse("environment_id", "id0").replace("id", "").toLong,
              runtimeName = imageName,
              runtimeVersion = imageVersion
            ),
            modelInfo = Try {
              ModelInstanceInfo(
                modelType = ModelType.fromTag(service.metadata.labels("model_type")),
                modelId = service.metadata.labels("model_version_id").toLong,
                modelName = service.metadata.labels("model_name"),
                modelVersion = service.metadata.labels("model_version").toLong,
                imageName = imageName,
                imageTag = imageVersion
              )
            }.toOption,
            instances = Seq(ServiceInstance(
              instanceId = service.metadata.uid,
              mainApplication = MainApplicationInstance(
                instanceId = service.metadata.uid,
                host = service.spec.map(_.clusterIP).getOrElse(""),
                port = service.spec.flatMap(_.ports.headOption).map(_.port).getOrElse(9091)
              ),
              sidecar = SidecarInstance(
                instanceId = service.metadata.uid,
                host = service.spec.map(_.clusterIP).getOrElse(""),
                ingressPort = service.spec.flatMap(_.ports.headOption).map(_.port).getOrElse(9091),
                egressPort = service.spec.flatMap(_.ports.headOption).map(_.port).getOrElse(9091),
                adminPort = service.spec.flatMap(_.ports.headOption).map(_.port).getOrElse(9091)
              ),
              model = if (service.metadata.labels.getOrElse("deployment_type", "") == "model") Some(ModelInstance(service.metadata.uid)) else None,
              advertisedHost = service.spec.map(_.clusterIP).getOrElse(""),
              advertisedPort = service.spec.flatMap(_.ports.headOption).map(_.port).getOrElse(9091)
            ))
          )
        })
      } yield cloudServices ++ createFakeHttpServices(cloudServices)
  }

  override def deployService(service: Service): Future[CloudService] = {
    import LabelSelector.dsl._

    val container = Container(service.serviceName, s"${service.runtime.name}:${service.runtime.version}")
      .exposePort(9090)
    
    val template = Pod.Template.Spec
      .named(service.serviceName)
      .addContainer(container)
      .addLabel("app" -> service.serviceName)
    
    val deployment = apps.v1.Deployment(metadata=ObjectMeta(name = service.serviceName, namespace = "serving"))
      .withReplicas(1)
      .withTemplate(template)
      .withLabelSelector("app" is service.serviceName)
    
    val kubeService = skuber.Service(metadata = ObjectMeta(name = service.serviceName, namespace = "serving"))
      .withSelector("app" -> service.serviceName)
      .exposeOnPort(skuber.Service.Port("application-port", Protocol.TCP, 9090))
      .addLabels(Map(
        "service_id" -> s"id${service.id}",
        "hs_service_marker" -> "hs_service_marker",
        "runtime_id" -> s"id${service.runtime.id}",
        "service_name" -> service.serviceName,
        "deployment_type" -> "model"
      ))
    
    val namespacedContext = k8s.usingNamespace("serving")
    
    (for {
      svc <- namespacedContext.create(kubeService)
      _ <- namespacedContext.create(deployment)
    } yield CloudService(
      id = service.id,
      serviceName = service.serviceName,
      statusText = "", // TODO: ?
      cloudDriverId = svc.metadata.uid,
      environmentName = service.environment.map(_.name),
      runtimeInfo = MainApplicationInstanceInfo(
        runtimeId = service.runtime.id,
        runtimeName = service.runtime.name,
        runtimeVersion = service.runtime.version
      ),
      modelInfo = service.model.map(m => {
        ModelInstanceInfo(
          modelType = m.modelType,
          modelId = m.id,
          modelName = m.modelName,
          modelVersion = m.modelVersion,
          imageName = m.imageName,
          imageTag = m.imageTag
        )
      }),
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
        advertisedPort = svc.spec.flatMap(_.ports.headOption).map(_.port).getOrElse(9090)
      ))
    )).map(cloudService => {
      internalManagerEventsPublisher.cloudServiceDetected(Seq(cloudService))
      cloudService
    })
  }

  override def services(serviceIds: Set[Long]): Future[Seq[CloudService]] = serviceList().map(_.filter(cs => serviceIds.contains(cs.id)))

  override def removeService(serviceId: Long): Future[Unit] = {
    import LabelSelector.dsl._
    val namespacedContext = k8s.usingNamespace("serving")
    for {
      svcList <- namespacedContext.listSelected[ServiceList]("service_id" is s"id$serviceId")
      svcName <- Future {
        svcList.headOption.map(_.metadata.name).getOrElse(throw new RuntimeException(s"kube service with id$serviceId not found"))
      }
      _ <- namespacedContext.delete[skuber.Service](svcName)
      _ <- namespacedContext.delete[apps.v1.Deployment](svcName)
    } yield Unit
  }
}
