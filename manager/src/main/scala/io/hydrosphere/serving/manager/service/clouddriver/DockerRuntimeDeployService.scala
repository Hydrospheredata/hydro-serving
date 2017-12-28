package io.hydrosphere.serving.manager.service.clouddriver

import java.util

import com.google.common.collect.ImmutableList
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.{ListContainersParam, RemoveContainerParam}
import com.spotify.docker.client.messages._
import io.hydrosphere.serving.manager.{DockerCloudDriverConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.model.{ModelServiceInstance, ModelServiceInstanceStatus}
import io.hydrosphere.serving.manager.model.ModelService
import org.apache.logging.log4j.scala.Logging

import collection.JavaConversions._

/**
  *
  */
class DockerRuntimeDeployService(
  dockerClient: DockerClient,
  managerConfiguration: ManagerConfiguration
) extends RuntimeDeployService with Logging {

  private val dockerCloudDriverConfiguration = managerConfiguration.cloudDriver.asInstanceOf[DockerCloudDriverConfiguration]

  override def deploy(runtime: ModelService, placeholders: Seq[Any]): String = {
    val conf = managerConfiguration.cloudDriver.asInstanceOf[DockerCloudDriverConfiguration]
    val labels = Map[String, String](
      LABEL_SERVICE_ID -> runtime.serviceId.toString,
      LABEL_HS_SERVICE_MARKER -> LABEL_HS_SERVICE_MARKER
    )

    val javaLabels = mapAsJavaMap(labels)

    val envMap = runtime.configParams ++ Map(
      ENV_HS_SERVICE_ID -> runtime.serviceName,
      ENV_APP_HTTP_PORT -> DEFAULT_APP_HTTP_PORT,
      ENV_SIDECAR_HTTP_PORT -> DEFAULT_SIDECAR_HTTP_PORT,
      ENV_SIDECAR_ADMIN_PORT -> DEFAULT_SIDECAR_ADMIN_PORT,
      ENV_MANAGER_HOST -> managerConfiguration.advertised.advertisedHost,
      ENV_MANAGER_PORT -> managerConfiguration.advertised.advertisedPort,
      ENV_ZIPKIN_ENABLED -> managerConfiguration.zipkin.enabled,
      ENV_ZIPKIN_HOST -> managerConfiguration.zipkin.host,
      ENV_ZIPKIN_PORT -> managerConfiguration.zipkin.port
    )

    val logConfig = dockerCloudDriverConfiguration.loggingGelfHost match {
      case None => null
      case Some(x) => LogConfig.create("gelf", Map("gelf-address" -> x))
    }

    val c = dockerClient.createContainer(ContainerConfig.builder()
      .hostConfig(HostConfig.builder()
        .logConfig(logConfig)
        .networkMode(conf.networkName).build()
      )
      .image(s"${runtime.modelRuntime.imageName}:${runtime.modelRuntime.imageMD5Tag}")
      .labels(javaLabels)
      .env(envMap.map { case (k, v) => s"$k=$v" }.toList)
      .build(), runtime.serviceName)
    dockerClient.startContainer(c.id())
    c.id()
  }


  def mapToServiceInfo(container: ContainerInfo): ServiceInfo = {
    ServiceInfo(
      id = container.config().labels().get(LABEL_SERVICE_ID).toLong,
      name = container.name(),
      cloudDriveId = container.id(),
      status = container.state().status(),
      statusText = container.state().error(),
      configParams = mapEnvironments(container.config().env())
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
      .foreach(s => dockerClient.removeContainer(s.id(), RemoveContainerParam.forceKill(true), RemoveContainerParam.removeVolumes(true)))

  override def serviceInstances(serviceId: Long): Seq[ModelServiceInstance] =
    serviceInstances(ListContainersParam.withLabel(LABEL_SERVICE_ID, serviceId.toString))

  private def getInstanceHost(list: util.Collection[AttachedNetwork]): String = {
    list.filter(n => n.aliases().contains(dockerCloudDriverConfiguration.networkName))
      .map(n => {
        val host = n.ipAddress()
        if (host.contains("/")) {
          host.substring(0, host.indexOf("/"))
        } else {
          host
        }
      }).head
  }

  private def mapEnvironments(list: ImmutableList[String]): Map[String, String] =
    list
      .map(p => p.split(":"))
      .filter(arr => arr.length > 1 && arr(0) != null && arr(1) != null)
      .map(arr => arr(0) -> arr(1)).toMap

  private def serviceInstances(criteria: ListContainersParam): Seq[ModelServiceInstance] = {
    val conf = managerConfiguration.cloudDriver.asInstanceOf[DockerCloudDriverConfiguration]
    dockerClient.listContainers(criteria).map(s => {
      try {
        val container = dockerClient.inspectContainer(s.id())
        val envMap = mapEnvironments(container.config().env())

        Some(ModelServiceInstance(
          instanceId = container.id(),
          host = container.networkSettings().networks().get(conf.networkName).ipAddress(),
          serviceId = container.config().labels().get(LABEL_SERVICE_ID).toLong,
          status = if (container.state().running()) {
            ModelServiceInstanceStatus.UP
          } else {
            ModelServiceInstanceStatus.DOWN
          },
          statusText = Option(s.status),
          appPort = envMap.getOrDefault(ENV_APP_HTTP_PORT, DEFAULT_APP_HTTP_PORT.toString).toInt,
          sidecarPort = envMap.getOrDefault(ENV_SIDECAR_HTTP_PORT, DEFAULT_SIDECAR_HTTP_PORT.toString).toInt,
          sidecarAdminPort = envMap.getOrDefault(ENV_SIDECAR_ADMIN_PORT, DEFAULT_SIDECAR_ADMIN_PORT.toString).toInt
        ))
      } catch {
        case ex: Throwable =>
          logger.error(s"Can't parse container $s", ex)
          None
      }
    }).filter(p => p.isDefined).flatten
  }
}
