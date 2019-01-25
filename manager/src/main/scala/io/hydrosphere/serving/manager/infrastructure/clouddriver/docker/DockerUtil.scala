package io.hydrosphere.serving.manager.infrastructure.clouddriver.docker

import java.util

import com.spotify.docker.client.messages.{Container, PortBinding}
import io.hydrosphere.serving.manager.config.SidecarConfig
import io.hydrosphere.serving.manager.domain.clouddriver._
import io.hydrosphere.serving.manager.domain.image.DockerImage

import scala.collection.JavaConverters._

object DockerUtil {
  def createPortBindingsMap(): util.Map[String, util.List[PortBinding]] = {
    val publishPorts = new util.HashMap[String, util.List[PortBinding]]()
    val bindingsList = new util.ArrayList[PortBinding]()
    bindingsList.add(PortBinding.randomPort("0.0.0.0"))
    publishPorts.put(DefaultConstants.DEFAULT_APP_PORT.toString, bindingsList)
    publishPorts
  }

  def collectCloudService(containers: Seq[Container], sidecarConfig: SidecarConfig): Seq[CloudService] = {
    containers
      .groupBy(_.labels().get(DefaultConstants.LABEL_SERVICE_ID))
      .filter {
        case (_, v) =>
          v.exists { c =>
            val depType = c.labels().get(DefaultConstants.LABEL_DEPLOYMENT_TYPE)
            depType == DefaultConstants.DEPLOYMENT_TYPE_APP && c.state() == "running"
          }
      }
      .flatMap {
        case (k, v) =>
          try {
            Seq(
              mapToCloudService(
                k.toLong,
                v.filterNot { c =>
                  val depType = c.labels().get(DefaultConstants.LABEL_DEPLOYMENT_TYPE)
                  depType == DefaultConstants.DEPLOYMENT_TYPE_APP && c.state() != "running"
                },
                sidecarConfig
              )
            )
          } catch {
            case e: Throwable =>
              Seq.empty
          }
      }.toSeq
  }

  def mapMainApplicationInstance(containerApp: Container, defaultHost: String): MainApplicationInstance = {
    MainApplicationInstance(
      instanceId = containerApp.id(),
      host = Option(containerApp.networkSettings().networks().get("bridge")).fold(defaultHost)(_.ipAddress()),
      port = containerApp.ports().asScala
        .filter(_.privatePort() == DefaultConstants.DEFAULT_APP_PORT)
        .find(_.publicPort() != null)
        .map(_.publicPort().toInt)
        .filter(_ != 0)
        .getOrElse(DefaultConstants.DEFAULT_APP_PORT)
    )
  }

  def mapToCloudService(serviceId: Long, seq: Seq[Container], sidecarConfig: SidecarConfig): CloudService = {
    val map = seq.map(c => c.labels().get(DefaultConstants.LABEL_DEPLOYMENT_TYPE) -> c).toMap

    val containerApp = map.getOrElse(DefaultConstants.DEPLOYMENT_TYPE_APP, throw new RuntimeException(s"Can't find APP for service $serviceId in $seq"))
    val containerModel = map.get(DefaultConstants.DEPLOYMENT_TYPE_MODEL)

    val mainApplicationInstance = mapMainApplicationInstance(containerApp, sidecarConfig.host)

    val image = containerApp.image()
    CloudService(
      id = serviceId,
      serviceName = Option(containerApp.labels().get(DefaultConstants.LABEL_SERVICE_NAME))
        .getOrElse(throw new RuntimeException(s"${DefaultConstants.LABEL_SERVICE_NAME} required $containerApp")),
      statusText = containerApp.status(),
      cloudDriverId = containerApp.id(),
      image = DockerImage(
        name = image,
        tag = image
      ),
      instances = Seq(
        ServiceInstance(
          advertisedHost = mainApplicationInstance.host,
          advertisedPort = mainApplicationInstance.port,
          instanceId = containerApp.id(),
          mainApplication = mainApplicationInstance,
          sidecar = SidecarInstance(
            instanceId = "managerConfiguration.sidecar",
            host = sidecarConfig.host,
            ingressPort = sidecarConfig.ingressPort,
            egressPort = sidecarConfig.egressPort,
            adminPort = sidecarConfig.adminPort
          ),
          model = containerModel.map(c => {
            ModelInstance(
              c.id()
            )
          })
        )
      )
    )
  }
}