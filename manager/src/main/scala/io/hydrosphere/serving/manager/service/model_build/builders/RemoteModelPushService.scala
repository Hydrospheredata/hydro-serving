package io.hydrosphere.serving.manager.service.model_build.builders

import com.spotify.docker.client.{DockerClient, ProgressHandler}
import io.hydrosphere.serving.manager.RemoteDockerRepositoryConfiguration
import io.hydrosphere.serving.manager.model.db.{ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.util.docker.{DockerClientHelper, DockerRegistryAuth}

class RemoteModelPushService(dockerClient: DockerClient, conf: RemoteDockerRepositoryConfiguration) extends ModelPushService {

  override def getImageName(modelBuild: ModelBuild): String = {
    s"${conf.host}/${modelBuild.model.name}:${modelBuild.version.toString}"
  }

  override def push(modelRuntime: ModelVersion, progressHandler: ProgressHandler): Unit = {
    dockerClient.push(s"${conf.host}/${modelRuntime.imageName}:${modelRuntime.imageTag}", DockerClientHelper.createRegistryAuth(DockerRegistryAuth(
      username = Some(conf.username),
      password = Some(conf.password),
      email = None,
      serverAddress = Some(conf.host),
      None,
      None
    )))
  }
}
