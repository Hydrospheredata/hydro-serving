package io.hydrosphere.serving.manager.service.modelbuild

import com.spotify.docker.client.DockerClient
import io.hydrosphere.serving.manager.model.ModelBuild


case class ProgressDetail(
  current: Long,
  start: Long,
  total: Long
)

case class ProgressMessage(
  id: String,
  status: String,
  stream: String,
  error: String,
  progress: String,
  progressDetail: Option[ProgressDetail]
)

trait ProgressHandler {
  def handle(progressMessage: ProgressMessage)
}

trait ModelBuildService {
  /**
    *
    * @param modelBuild
    * @param progressHandler
    * @return image md5 tag
    */
  def build(modelBuild: ModelBuild, progressHandler: ProgressHandler): String
}


class DefaultModelBuildService(dockerClient: DockerClient) extends ModelBuildService {

  override def build(modelBuild: ModelBuild, progressHandler: ProgressHandler): String = {
    //dockerClient.build()

    ""
  }
}