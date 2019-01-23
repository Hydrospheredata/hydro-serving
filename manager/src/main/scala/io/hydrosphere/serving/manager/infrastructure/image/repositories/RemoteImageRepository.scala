package io.hydrosphere.serving.manager.infrastructure.image.repositories

import com.spotify.docker.client.messages.RegistryAuth
import com.spotify.docker.client.{DockerClient, ProgressHandler}
import io.hydrosphere.serving.manager.config.DockerRepositoryConfiguration
import io.hydrosphere.serving.manager.domain.image.{DockerImage, ImageRepository}
import io.hydrosphere.serving.manager.util.docker.{DockerClientHelper, DockerRegistryAuth}

import scala.concurrent.{ExecutionContext, Future}

class RemoteImageRepository(
  dockerClient: DockerClient,
  conf: DockerRepositoryConfiguration.Remote,
  progressHandler: ProgressHandler
)(implicit ec: ExecutionContext)
  extends ImageRepository[Future] {

  override def push(dockerImage: DockerImage): Future[Unit] = Future {
    val auth: RegistryAuth = if (conf.username.isEmpty && conf.password.isEmpty) {
      RegistryAuth.fromDockerConfig(conf.host).build()
    } else {
      DockerClientHelper.createRegistryAuth(DockerRegistryAuth(
        username = conf.username,
        password = conf.password,
        email = None,
        serverAddress = Some(conf.host),
        None,
        None
      ))
    }
    dockerClient.push(dockerImage.fullName, auth) // TODO ???
  }

  override def getImage(name: String, tag: String): DockerImage = {
    DockerImage(
      name = s"${conf.host}/$name",
      tag = tag
    )
  }
}