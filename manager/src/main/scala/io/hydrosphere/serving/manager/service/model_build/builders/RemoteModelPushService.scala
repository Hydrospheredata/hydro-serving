package io.hydrosphere.serving.manager.service.model_build.builders

import com.spotify.docker.client.messages.RegistryAuth
import com.spotify.docker.client.{DockerClient, ProgressHandler}
import io.hydrosphere.serving.manager.config.DockerRepositoryConfiguration
import io.hydrosphere.serving.manager.model.db.{ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.util.docker.{DockerClientHelper, DockerRegistryAuth}

class RemoteModelPushService(dockerClient: DockerClient, conf: DockerRepositoryConfiguration.Remote) extends ModelPushService {

  override def getImageName(modelBuild: ModelBuild): String = {
    s"${conf.host}/${modelBuild.model.name}:${modelBuild.version.toString}"
  }

  override def push(modelRuntime: ModelVersion, progressHandler: ProgressHandler): Unit = {
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
    dockerClient.push(s"${conf.host}/${modelRuntime.imageName}:${modelRuntime.imageTag}", auth)
  }
}
