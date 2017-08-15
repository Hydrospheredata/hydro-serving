package io.hydrosphere.serving.manager.service.modelbuild

import java.io.ByteArrayInputStream
import java.nio.file._

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.BuildParam
import com.spotify.docker.client.messages.RegistryAuth
import io.hydrosphere.serving.manager.model.{ModelBuild, ModelRuntime}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.commons.io.FileUtils


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

case class DockerRegistryAuth(
  username: Option[String],
  password: Option[String],
  email: Option[String],
  serverAddress: Option[String],
  identityToken: Option[String],
  auth: Option[String]
)

trait ProgressHandler {
  def handle(progressMessage: ProgressMessage)
}

trait ModelBuildService {
  val SCRIPT_VAL_MODEL_PATH = "MODEL_PATH"
  val SCRIPT_VAL_RUNTIME_IMAGE = "RUNTIME_IMAGE"
  val SCRIPT_VAL_RUNTIME_VERSION = "RUNTIME_VERSION"
  val SCRIPT_VAL_MODEL_NAME = "MODEL_NAME"
  val SCRIPT_VAL_MODEL_VERSION = "MODEL_VERSION"

  /**
    *
    * @param modelBuild
    *
    * @param progressHandler
    * @return image md5 tag
    */
  def build(modelBuild: ModelBuild, script: String, progressHandler: ProgressHandler): String
}

trait ModelPushService {
  def push(modelRuntime: ModelRuntime, progressHandler: ProgressHandler)
}

class EmptyModelPushService extends ModelPushService {
  override def push(modelRuntime: ModelRuntime, progressHandler: ProgressHandler): Unit = Unit
}