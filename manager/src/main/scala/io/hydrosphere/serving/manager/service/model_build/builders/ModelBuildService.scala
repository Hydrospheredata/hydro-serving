package io.hydrosphere.serving.manager.service.model_build.builders

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{ModelBuild, ModelVersion}

import scala.concurrent.Future


case class ProgressDetail(
  current: Option[Long],
  start: Option[Long],
  total: Option[Long]
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
  val SCRIPT_VAL_MODEL_TYPE = "MODEL_TYPE"
  val SCRIPT_VAL_MODEL_NAME = "MODEL_NAME"
  val SCRIPT_VAL_MODEL_VERSION = "MODEL_VERSION"

  def build(modelBuild: ModelBuild, imageName: String, script: String, progressHandler: ProgressHandler): HFResult[String]
}

trait ModelPushService {
  def getImageName(modelBuild: ModelBuild): String = {
    modelBuild.model.name
  }

  def push(modelRuntime: ModelVersion, progressHandler: ProgressHandler)
}

class EmptyModelPushService extends ModelPushService {
  override def push(modelRuntime: ModelVersion, progressHandler: ProgressHandler): Unit = {}
}

object DockerClientHelper {

  def createProgressHandlerWrapper(progressHandler: ProgressHandler): com.spotify.docker.client.ProgressHandler = {
    new com.spotify.docker.client.ProgressHandler {
      override def progress(progressMessage: com.spotify.docker.client.messages.ProgressMessage): Unit = {
        progressHandler.handle(ProgressMessage(
          id = progressMessage.id(),
          status = progressMessage.status(),
          stream = progressMessage.stream(),
          error = progressMessage.error(),
          progress = progressMessage.progress(),
          progressDetail = {
            Option(progressMessage.progressDetail()).map(d =>
              ProgressDetail(
                current = Option(d.current()).map(_.toLong),
                start = Option(d.start()).map(_.toLong),
                total = Option(d.total()).map(_.toLong)
              )
            )
          }
        ))
      }
    }
  }

  def createRegistryAuth(registryAuth: DockerRegistryAuth): com.spotify.docker.client.messages.RegistryAuth = {
    com.spotify.docker.client.messages.RegistryAuth.create(
      registryAuth.username.orNull,
      registryAuth.password.orNull,
      registryAuth.email.orNull,
      registryAuth.serverAddress.orNull,
      registryAuth.identityToken.orNull,
      registryAuth.auth.orNull
    )
  }
}