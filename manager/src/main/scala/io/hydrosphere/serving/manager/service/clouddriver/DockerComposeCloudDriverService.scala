package io.hydrosphere.serving.manager.service.clouddriver

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.{Container, HostConfig, LogConfig}
import io.hydrosphere.serving.manager.{DockerCloudDriverConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.service.InternalManagerEventsPublisher

import collection.JavaConversions._
import scala.concurrent.ExecutionContext

class DockerComposeCloudDriverService(
  dockerClient: DockerClient,
  managerConfiguration: ManagerConfiguration,
  internalManagerEventsPublisher: InternalManagerEventsPublisher
) (
  implicit override val ex: ExecutionContext
) extends LocalCloudDriverService(dockerClient, managerConfiguration, internalManagerEventsPublisher) {

  private val driverConfiguration = managerConfiguration.cloudDriver.asInstanceOf[DockerCloudDriverConfiguration]

  override protected def postProcessAllServiceList(services: Seq[CloudService]): Seq[CloudService] = {
    val manager = services.find(_.id == CloudDriverService.MANAGER_ID)
      .getOrElse(throw new RuntimeException(s"Can't find manager in $services"))

    services.filter(_.id != CloudDriverService.MANAGER_ID) :+ createManagementHttp(manager) :+ createManagement(manager)
  }

  override protected def createMainApplicationHostConfigBuilder(): HostConfig.Builder = {
    val builder = HostConfig.builder()
      .networkMode(driverConfiguration.networkName)

    driverConfiguration.loggingGelfHost match {
      case Some(x) =>
        builder.logConfig(LogConfig.create("gelf", Map("gelf-address" -> x)))
      case _ =>
    }
    builder
  }


  private def createManagementHttp(management: CloudService): CloudService =
    management.copy(
      id = CloudDriverService.MANAGER_HTTP_ID,
      serviceName = CloudDriverService.MANAGER_HTTP_NAME,
      instances = management.instances.map(s =>
        s.copy(
          advertisedPort = managerConfiguration.application.port,
          mainApplication = s.mainApplication.copy(port = managerConfiguration.application.port)
        )
      )
    )

  private def createManagement(management: CloudService): CloudService =
    management.copy(
      instances = management.instances.map(s =>
        s.copy(
          advertisedPort = managerConfiguration.application.grpcPort,
          mainApplication = s.mainApplication.copy(port = managerConfiguration.application.grpcPort)
        )
      )
    )


  override protected def mapMainApplicationInstance(containerApp: Container): MainApplicationInstance =
    MainApplicationInstance(
      instanceId = containerApp.id(),
      host = Option(containerApp.networkSettings().networks().get(driverConfiguration.networkName))
        .map(n => n.ipAddress())
        .getOrElse(containerApp.networkSettings().ipAddress()),
      port = DEFAULT_APP_PORT
    )
}
