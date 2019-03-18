package io.hydrosphere.serving.manager.util.docker


import com.spotify.docker.client.{DefaultDockerClient, DockerClient, ProgressHandler}
import com.spotify.docker.client.exceptions.DockerCertificateException
import com.spotify.docker.client.exceptions.DockerException
import com.spotify.docker.client.messages.ContainerConfig
import com.spotify.docker.client.messages.ContainerCreation
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.ConcurrentSkipListSet

import scala.collection.JavaConverters._
import com.spotify.docker.client.DockerClient.RemoveContainerParam


object IsolatedDockerClient {
  def createFromEnv = new IsolatedDockerClient(DefaultDockerClient.fromEnv)
}

class IsolatedDockerClient private(val builder: DefaultDockerClient.Builder) extends DefaultDockerClient(builder) {
  private val imageStorage = new ConcurrentSkipListSet[String]
  private val containerStorage = new ConcurrentSkipListSet[String]

  override def build(directory: Path, name: String, dockerfile: String, handler: ProgressHandler, params: DockerClient.BuildParam*): String = {
    val image = super.build(directory, name, dockerfile, handler, params:_*)
    imageStorage.add(image)
    image
  }

  override def removeContainer(containerId: String, params: RemoveContainerParam*): Unit = {
    super.removeContainer(containerId, params: _*)
    containerStorage.remove(containerId)
  }

  override def createContainer(config: ContainerConfig, name: String): ContainerCreation = {
    val creation = super.createContainer(config, name)
    containerStorage.add(creation.id)
    creation
  }

  def clear(): Unit = {
    for (container <- containerStorage.asScala) {
      try {
        super.removeContainer(container, RemoveContainerParam.forceKill)
        containerStorage.remove(container)
      } catch {
        case e@(_: DockerException | _: InterruptedException) =>
          e.printStackTrace()
      }
    }
    for (image <- imageStorage.asScala) {
      try {
        super.removeImage(image, true, true)
        imageStorage.remove(image)
      } catch {
        case e@(_: DockerException | _: InterruptedException) =>
          e.printStackTrace()
      }
    }
  }

  override def close(): Unit = {
    clear()
    super.close()
  }
}
