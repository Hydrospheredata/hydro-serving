package io.hydrosphere.serving.manager.service.clouddriver

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.{Container, HostConfig, LogConfig}
import io.hydrosphere.serving.manager.service.internal_events.InternalManagerEventsPublisher

import collection.JavaConverters._
import scala.concurrent.ExecutionContext
import CloudDriverService._
import io.hydrosphere.serving.manager.config.{CloudDriverConfiguration, ManagerConfiguration}

class DockerComposeCloudDriverService(
  dockerClient: DockerClient,
  managerConfiguration: ManagerConfiguration,
  internalManagerEventsPublisher: InternalManagerEventsPublisher
)(
  implicit override val ex: ExecutionContext
) extends LocalCloudDriverService(dockerClient, managerConfiguration, internalManagerEventsPublisher) {

  private val driverConfiguration = managerConfiguration.cloudDriver.asInstanceOf[CloudDriverConfiguration.Docker]

  override protected def postProcessAllServiceList(services: Seq[CloudService]): Seq[CloudService] = {
    services ++ createFakeHttpServices(services)
  }

  override protected def createMainApplicationHostConfigBuilder(): HostConfig.Builder = {
    val builder = HostConfig.builder()
      .networkMode(driverConfiguration.networkName)

    driverConfiguration.loggingConfiguration match {
      case Some(x) =>
        builder.logConfig(LogConfig.create(x.driver, x.params.asJava))
      case _ =>
    }
    builder
  }

  override protected def mapMainApplicationInstance(containerApp: Container): MainApplicationInstance =
    MainApplicationInstance(
      instanceId = containerApp.id(),
      host = Option(containerApp.networkSettings().networks().get(driverConfiguration.networkName))
        .map(n => n.ipAddress())
        .getOrElse(containerApp.networkSettings().ipAddress()),
      port = DEFAULT_APP_PORT
    )
}
