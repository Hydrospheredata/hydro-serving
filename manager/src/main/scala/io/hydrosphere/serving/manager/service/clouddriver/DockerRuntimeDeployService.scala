package io.hydrosphere.serving.manager.service.clouddriver

import java.util
import java.util.Collections

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.{ListContainersParam, RemoveContainerParam}
import com.spotify.docker.client.messages._
import io.hydrosphere.serving.manager.{DockerCloudDriverConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.model.{ModelService, ModelServiceInstance, ModelServiceInstanceStatus}
import org.apache.logging.log4j.scala.Logging

import collection.JavaConversions._

/**
  *
  */
class DockerRuntimeDeployService(
  dockerClient: DockerClient,
  managerConfiguration: ManagerConfiguration
) extends RuntimeDeployService with Logging {

  override def deploy(runtime: ModelService): String ={
    val conf = managerConfiguration.cloudDriver.asInstanceOf[DockerCloudDriverConfiguration]
    val labels = Map[String, String](
      LABEL_SERVICE_ID -> runtime.serviceId.toString,
      LABEL_HS_SERVICE_MARKER -> LABEL_HS_SERVICE_MARKER
    )

    val javaLabels = mapAsJavaMap(labels)

    val env = List[String](
      s"$ENV_HS_SERVICE_ID=${runtime.serviceId}",

      s"$ENV_APP_HTTP_PORT=9090",
      s"$ENV_SIDECAR_HTTP_PORT=8080",

      s"$ENV_MANAGER_HOST=${managerConfiguration.advertised.advertisedHost}",
      s"$ENV_MANAGER_PORT=${managerConfiguration.advertised.advertisedPort.toString}",
      s"$ENV_ZIPKIN_ENABLED=${managerConfiguration.zipkin.enabled.toString}",
      s"$ENV_ZIPKIN_HOST=${managerConfiguration.zipkin.host}",
      s"$ENV_ZIPKIN_PORT=${managerConfiguration.zipkin.port.toString}"
    )

    val c=dockerClient.createContainer(ContainerConfig.builder()
        .image(s"${runtime.modelRuntime.imageName}:${runtime.modelRuntime.imageMD5Tag}")
        .labels(javaLabels)
        .hostname(runtime.serviceName)
        .env(env)
        .build(),runtime.serviceName)
    dockerClient.startContainer(c.id())
    c.id()
  }



  def mapToServiceInfo(container: ContainerInfo): ServiceInfo = {
    ServiceInfo(
      id = container.config().labels().get(LABEL_SERVICE_ID).toLong,
      name = container.name(),
      cloudDriveId = container.id(),
      status = container.state().status(),
      statusText = container.state().error()
    )
  }

  override def serviceList(): Seq[ServiceInfo] =
    dockerClient.listContainers(ListContainersParam.withLabel(LABEL_HS_SERVICE_MARKER, LABEL_HS_SERVICE_MARKER))
      .map(s => {
        val container = dockerClient.inspectContainer(s.id())
        mapToServiceInfo(container)
      })

  override def service(serviceId: Long): Option[ServiceInfo] =
    dockerClient.listContainers(ListContainersParam.withLabel(LABEL_SERVICE_ID, serviceId.toString))
      .headOption
      .map(s => {
        val container = dockerClient.inspectContainer(s.id())
        mapToServiceInfo(container)
      })

  override def deleteService(serviceId: Long): Unit =
    dockerClient.listContainers(ListContainersParam.withLabel(LABEL_SERVICE_ID, serviceId.toString))
      .foreach(s => dockerClient.removeContainer(s.id(), RemoveContainerParam.forceKill()))

  override def serviceInstances(): Seq[ModelServiceInstance] =
    serviceInstances(ListContainersParam.withLabel(LABEL_HS_SERVICE_MARKER, LABEL_HS_SERVICE_MARKER))

  override def serviceInstances(serviceId: Long): Seq[ModelServiceInstance] =
    serviceInstances(ListContainersParam.withLabel(LABEL_SERVICE_ID, serviceId.toString))

  private def getInstanceHost(list: util.Collection[AttachedNetwork]): String = {
    val conf = managerConfiguration.cloudDriver.asInstanceOf[DockerCloudDriverConfiguration]
    list.filter(n => n.aliases().contains(conf.networkName))
      .map(n => {
        val host = n.ipAddress()
        if (host.contains("/")) {
          host.substring(0, host.indexOf("/"))
        } else {
          host
        }
      }).head
  }

  private def serviceInstances(criteria: ListContainersParam): Seq[ModelServiceInstance] ={
    val conf = managerConfiguration.cloudDriver.asInstanceOf[DockerCloudDriverConfiguration]
    dockerClient.listContainers(criteria).map(s => {
      val container = dockerClient.inspectContainer(s.id())
      val envMap = container.config().env()
        .map(p => p.split(":"))
        .filter(arr => arr.length > 1 && arr(0) != null && arr(1) != null)
        .map(arr => arr(0) -> arr(1)).toMap

      ModelServiceInstance(
        instanceId = container.id(),
        host = container.networkSettings().networks().get(conf.networkName).ipAddress(),
        serviceId = container.config().labels().get(LABEL_SERVICE_ID).toLong,
        status = if (container.state().running()) {
          ModelServiceInstanceStatus.UP
        } else {
          ModelServiceInstanceStatus.DOWN
        },
        statusText = s.status,
        appPort = envMap.getOrDefault(ENV_APP_HTTP_PORT, "9090").toInt,
        sidecarPort = envMap.getOrDefault(ENV_SIDECAR_HTTP_PORT, "8080").toInt
      )
    })
  }
}
