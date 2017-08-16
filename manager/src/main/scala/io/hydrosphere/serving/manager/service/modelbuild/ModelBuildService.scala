package io.hydrosphere.serving.manager.service.modelbuild

import io.hydrosphere.serving.manager.model.{ModelBuild, ModelRuntime}


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
  def build(modelBuild: ModelBuild, imageName:String,  script: String, progressHandler: ProgressHandler): String

}

trait ModelPushService {
  def getImageName(modelBuild: ModelBuild): String = {
    modelBuild.model.name
  }

  def push(modelRuntime: ModelRuntime, progressHandler: ProgressHandler)
}

class EmptyModelPushService extends ModelPushService {
  override def push(modelRuntime: ModelRuntime, progressHandler: ProgressHandler): Unit = {}
}

object DockerClientHelper {

  def createProgressHadlerWrapper(progressHandler: ProgressHandler): com.spotify.docker.client.ProgressHandler = {
    new com.spotify.docker.client.ProgressHandler {
      override def progress(progressMessage: com.spotify.docker.client.messages.ProgressMessage): Unit = {
        progressHandler.handle(ProgressMessage(
          id = progressMessage.id(),
          status = progressMessage.status(),
          stream = progressMessage.stream(),
          error = progressMessage.error(),
          progress = progressMessage.progress(),
          progressDetail = {
            if (progressMessage.progressDetail() != null) {
              Some(ProgressDetail(
                current = progressMessage.progressDetail().current(),
                start = progressMessage.progressDetail().start(),
                total = progressMessage.progressDetail().total()
              ))
            } else {
              None
            }
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