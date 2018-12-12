package io.hydrosphere.serving.manager.infrastructure.clouddriver

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.{Container, HostConfig, LogConfig}
import io.hydrosphere.serving.manager.config.{CloudDriverConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.domain.clouddriver.{CloudService, DefaultConstants, MainApplicationInstance}
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.InternalManagerEventsPublisher

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext
import DefaultConstants._

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
      port = DefaultConstants.DEFAULT_APP_PORT
    )
}