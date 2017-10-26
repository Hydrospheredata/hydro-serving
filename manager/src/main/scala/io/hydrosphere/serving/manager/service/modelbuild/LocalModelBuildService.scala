package io.hydrosphere.serving.manager.service.modelbuild

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path}

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.BuildParam
import io.hydrosphere.serving.manager.model.ModelBuild
import io.hydrosphere.serving.manager.service.SourceManagementService
import org.apache.commons.io.FileUtils

/**
  *
  */
class LocalModelBuildService(
  dockerClient: DockerClient,
  sourceManagementService: SourceManagementService
) extends ModelBuildService {
  private val modelDir = "model"

  override def build(modelBuild: ModelBuild, imageName:String, script: String, progressHandler: ProgressHandler): String = {
    val modelSource = sourceManagementService.getLocalPath(modelBuild.model.source)
    val dockerFile = script.replaceAll("\\{" + SCRIPT_VAL_MODEL_PATH + "\\}", modelDir)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_VERSION + "\\}", modelBuild.modelVersion)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_NAME + "\\}", modelBuild.model.name)
      .replaceAll("\\{" + SCRIPT_VAL_RUNTIME_IMAGE + "\\}", modelBuild.model.runtimeType.get.name)
      .replaceAll("\\{" + SCRIPT_VAL_RUNTIME_VERSION + "\\}", modelBuild.model.runtimeType.get.version)

    val tmpPath = Files.createTempDirectory(s"hydroserving-${modelBuild.id}")
    try {
      build(tmpPath, modelSource, dockerFile, progressHandler, modelBuild, imageName)
    } catch {
      case ex: Throwable =>
        tmpPath.toFile.delete()
        throw ex
    }
  }

  private def build(buildPath: Path, model: Path, dockerFile: String, progressHandler: ProgressHandler, modelBuild: ModelBuild, imageName:String): String = {
    Files.copy(new ByteArrayInputStream(dockerFile.getBytes), buildPath.resolve("Dockerfile"))
    FileUtils.copyDirectory(model.toFile, buildPath.resolve(s"$modelDir/").toFile)
    val res=dockerClient.build(
      buildPath,
      s"$imageName:${modelBuild.modelVersion}",
      "Dockerfile",
      DockerClientHelper.createProgressHadlerWrapper(progressHandler),
      BuildParam.noCache()
    )
    if(res==null){
      throw new RuntimeException("Can't build model")
    }
    res
  }
}