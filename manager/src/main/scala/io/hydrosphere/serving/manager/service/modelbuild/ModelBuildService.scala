package io.hydrosphere.serving.manager.service.modelbuild

import java.io.ByteArrayInputStream
import java.nio.file._
import java.nio.file.attribute.FileAttribute

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.BuildParam
import com.spotify.docker.client.messages.RegistryAuth
import io.hydrosphere.serving.manager.model.{ModelBuild, ModelRuntime}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource


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

  def push(modelRuntime: ModelRuntime, progressHandler: ProgressHandler, dockerRegistryAuth: Option[DockerRegistryAuth])
}


class DefaultModelBuildService(
  dockerClient: DockerClient,
  modelSources: Seq[ModelSource]
) extends ModelBuildService {
  private val modelDir="model"


  private def findModelSource(prefix: String): ModelSource =
    modelSources
      .find(s => s.getSourcePrefix() == prefix)
      .getOrElse(throw new IllegalArgumentException(s"Can't find ModelSource for prefix $prefix"))


  override def build(modelBuild: ModelBuild, script: String, progressHandler: ProgressHandler): String = {
    val modelSource = findModelSource(modelBuild.model.source.split(":").head)
    val dockerFile = script.replaceAll("\\{" + SCRIPT_VAL_MODEL_PATH + "\\}", modelDir)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_VERSION + "\\}", modelBuild.modelVersion)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_NAME+ "\\}", modelBuild.model.name)
      .replaceAll("\\{" + SCRIPT_VAL_RUNTIME_IMAGE+ "\\}", modelBuild.model.runtimeType.get.name)
      .replaceAll("\\{" + SCRIPT_VAL_RUNTIME_VERSION+ "\\}", modelBuild.model.runtimeType.get.version)

    val tmpPath = Files.createTempDirectory(s"hydroserving-${modelBuild.id}")
    try {
      build(tmpPath, modelSource.getLocalCopy(modelBuild.model.source), dockerFile, progressHandler, modelBuild)
    } catch {
      case ex: Throwable =>
        tmpPath.toFile.delete()
        throw ex
    }
  }

  private def build(buildPath: Path, model: Path, dockerFile: String, progressHandler: ProgressHandler, modelBuild: ModelBuild): String = {
    Files.copy(new ByteArrayInputStream(dockerFile.getBytes), buildPath.resolve("Dockerfile"))
    Files.copy(model, buildPath.resolve(s"$modelDir/"))
    dockerClient.build(
      buildPath,
      s"${modelBuild.model.name}:${modelBuild.modelVersion}",
      "Dockerfile",
      createProgressHadlerWrapper(progressHandler),
      BuildParam.noCache()
    )
  }

  private def createProgressHadlerWrapper(progressHandler: ProgressHandler): com.spotify.docker.client.ProgressHandler = {
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

  override def push(modelRuntime: ModelRuntime, progressHandler: ProgressHandler, dockerRegistryAuth: Option[DockerRegistryAuth]): Unit =
    dockerRegistryAuth match {
      case None => dockerClient.push(s"${modelRuntime.imageName}:${modelRuntime.imageMD5Tag}", createProgressHadlerWrapper(progressHandler))
      case Some(x) => dockerClient.push(s"${modelRuntime.imageName}:${modelRuntime.imageMD5Tag}", createProgressHadlerWrapper(progressHandler),
        RegistryAuth.create(
          x.username.orNull,
          x.password.orNull,
          x.email.orNull,
          x.serverAddress.orNull,
          x.identityToken.orNull,
          x.auth.orNull
        ))
    }
}