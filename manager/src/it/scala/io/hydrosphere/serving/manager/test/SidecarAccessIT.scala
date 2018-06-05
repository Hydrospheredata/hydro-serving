package io.hydrosphere.serving.manager.test

import java.net.InetAddress

import com.spotify.docker.client.messages.{ContainerConfig, HostConfig, PortBinding}

import scala.collection.JavaConverters._

trait SidecarAccessIT extends IsolatedDockerAccessIT {
  def hostIp = InetAddress.getLocalHost.getHostAddress

  def sidecarConfig = ContainerConfig.builder()
    .image("hydrosphere/serving-sidecar:latest")
    .hostConfig(
      HostConfig.builder().portBindings(
        Map(
          "8080/tcp" -> List(
            PortBinding.of("", 8080)
          ).asJava,
          "8081/tcp" -> List(
            PortBinding.of("", 8081)
          ).asJava,
          "8082/tcp" -> List(
            PortBinding.of("", 8082)
          ).asJava
        ).asJava
      ).build()
    )
    .exposedPorts("8080/tcp", "8081/tcp", "8082/tcp")
    .env(
      s"MANAGER_HOST=$hostIp",
      "MANAGER_PORT=9091",
      "SERVICE_NAME=manager",
      "SERVICE_ID=-20"
    )
    .build()

  def sidecarContainer = dockerClient.createContainer(sidecarConfig)
  logger.info("Created sidecar container")

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    dockerClient.startContainer(sidecarContainer.id())
    logger.info("Starting sidecar container")
  }
}
