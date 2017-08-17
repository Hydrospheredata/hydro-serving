package io.hydrosphere.serving.manager.service.modelbuild

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path}

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.BuildParam
import io.hydrosphere.serving.manager.model.{ModelBuild, ModelRuntime}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.commons.io.FileUtils

/**
  *
  */
class LocalModelBuildService(
  dockerClient: DockerClient,
  modelSources: Seq[ModelSource]
) extends ModelBuildService {
  private val modelDir = "model"


  private def findModelSource(prefix: String): ModelSource =
    modelSources
      .find(s => s.getSourcePrefix() == prefix)
      .getOrElse(throw new IllegalArgumentException(s"Can't find ModelSource for prefix $prefix"))


  override def build(modelBuild: ModelBuild, script: String, progressHandler: ProgressHandler): String = {
    val modelSource = findModelSource(modelBuild.model.source.split(":").head)
    val dockerFile = script.replaceAll("\\{" + SCRIPT_VAL_MODEL_PATH + "\\}", modelDir)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_VERSION + "\\}", modelBuild.modelVersion)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_NAME + "\\}", modelBuild.model.name)
      .replaceAll("\\{" + SCRIPT_VAL_RUNTIME_IMAGE + "\\}", modelBuild.model.runtimeType.get.name)
      .replaceAll("\\{" + SCRIPT_VAL_RUNTIME_VERSION + "\\}", modelBuild.model.runtimeType.get.version)

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
    FileUtils.copyDirectory(model.toFile, buildPath.resolve(s"$modelDir/").toFile)
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
}