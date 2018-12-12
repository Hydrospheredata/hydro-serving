package io.hydrosphere.serving.manager.infrastructure.model.push

import com.spotify.docker.client.messages.RegistryAuth
import com.spotify.docker.client.{DockerClient, ProgressHandler}
import io.hydrosphere.serving.manager.config.DockerRepositoryConfiguration
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionPushAlgebra}
import io.hydrosphere.serving.manager.util.docker.{DockerClientHelper, DockerRegistryAuth}

class RemoteModelPushService(dockerClient: DockerClient, conf: DockerRepositoryConfiguration.Remote) extends ModelVersionPushAlgebra {

  override def getImage(modelName: String, modelVersion: Long): DockerImage = {
    DockerImage(
      name = s"${conf.host}/$modelName",
      tag = modelVersion.toString
    )
  }

  override def push(modelVersion: ModelVersion, progressHandler: ProgressHandler): Unit = {
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
    dockerClient.push(s"${conf.host}/${modelVersion.runtime.fullName}", auth) // TODO ???
  }
}