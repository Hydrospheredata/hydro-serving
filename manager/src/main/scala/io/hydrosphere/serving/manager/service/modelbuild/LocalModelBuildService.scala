package io.hydrosphere.serving.manager.service.modelbuild

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path}

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.BuildParam
import io.hydrosphere.serving.manager.model.db.ModelBuild
import io.hydrosphere.serving.manager.service.SourceManagementService
import org.apache.commons.io.FileUtils

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class LocalModelBuildService(
  dockerClient: DockerClient,
  sourceManagementService: SourceManagementService
)(implicit val ex: ExecutionContext) extends ModelBuildService {
  private val modelRootDir = "model"
  private val modelFilesDir = s"$modelRootDir/files/"
  private val contractFile = s"$modelRootDir/contract.protobin"

  override def build(modelBuild: ModelBuild, imageName: String, script: String, progressHandler: ProgressHandler): Future[String] = {
    sourceManagementService.getLocalPath(modelBuild.model.source).map { localModelPath =>
      val dockerFile = script
        .replaceAll("\\{"  +   SCRIPT_VAL_MODEL_PATH     +  "\\}", modelRootDir)
        .replaceAll("\\{"  +   SCRIPT_VAL_MODEL_VERSION  +  "\\}", modelBuild.modelVersion.toString)
        .replaceAll("\\{"  +   SCRIPT_VAL_MODEL_NAME     +  "\\}", modelBuild.model.name)
        .replaceAll("\\{"  +   SCRIPT_VAL_MODEL_TYPE     +  "\\}", modelBuild.model.modelType.toTag)

      val tmpBuildPath = Files.createTempDirectory(s"hydroserving-${modelBuild.id}")
      try {
        build(tmpBuildPath, localModelPath, dockerFile, progressHandler, modelBuild, imageName)
      } catch {
        case ex: Throwable =>
          tmpBuildPath.toFile.delete()
          throw ex
      }
    }
  }

  private def build(buildPath: Path, model: Path, dockerFile: String, progressHandler: ProgressHandler, modelBuild: ModelBuild, imageName: String): String = {
    Files.copy(new ByteArrayInputStream(dockerFile.getBytes), buildPath.resolve("Dockerfile"))
    Files.createDirectories(buildPath.resolve(modelRootDir))
    FileUtils.copyDirectory(model.toFile, buildPath.resolve(modelFilesDir).toFile)
    Files.write(buildPath.resolve(contractFile), modelBuild.model.modelContract.toByteArray)

    val dockerContainer = Option {
      dockerClient.build(
        buildPath,
        s"$imageName:${modelBuild.version}",
        "Dockerfile",
        DockerClientHelper.createProgressHandlerWrapper(progressHandler),
        BuildParam.noCache()
      )
    }.getOrElse(throw new RuntimeException("Can't build model"))

    dockerClient.inspectImage(dockerContainer).id().stripPrefix("sha256:")
  }

}