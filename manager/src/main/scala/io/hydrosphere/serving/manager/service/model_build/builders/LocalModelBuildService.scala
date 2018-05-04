package io.hydrosphere.serving.manager.service.model_build.builders

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path}

import cats.data.EitherT
import cats.implicits._
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.BuildParam
import com.spotify.docker.client.exceptions.DockerException
import io.hydrosphere.serving.manager.model.{HFResult, Result}
import io.hydrosphere.serving.manager.model.db.ModelBuild
import org.apache.commons.io.FileUtils
import io.hydrosphere.serving.manager.model.Result.Implicits._
import io.hydrosphere.serving.manager.service.source.{SourceManagementService, SourcePath}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class LocalModelBuildService(
  dockerClient: DockerClient,
  sourceManagementService: SourceManagementService
)(implicit val ex: ExecutionContext) extends ModelBuildService {
  private val modelRootDir = "model"
  private val modelFilesDir = s"$modelRootDir/files/"
  private val contractFile = s"$modelRootDir/contract.protobin"

  override def build(modelBuild: ModelBuild, imageName: String, script: String, progressHandler: ProgressHandler): HFResult[String] = {
    val tmpBuildPath = Files.createTempDirectory(s"hydroserving-${modelBuild.id}")
    val fT = for {
      sourcePath <- EitherT(Future.successful(SourcePath.parse(modelBuild.model.source)))
      localPath <- EitherT(sourceManagementService.getLocalPath(sourcePath))
      dockerFile = prepareScript(modelBuild, script)
      buildRes <- EitherT(build(tmpBuildPath, localPath, dockerFile, progressHandler, modelBuild, imageName))
    } yield buildRes
    fT.value
  }

  private def build(buildPath: Path, model: Path, dockerFile: String, progressHandler: ProgressHandler, modelBuild: ModelBuild, imageName: String): HFResult[String] = {
    try {
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
      }.toHResult(Result.InternalError(new RuntimeException("Can't build docker container")))

      Future.successful(
        dockerContainer.right.map { container =>
          dockerClient.inspectImage(container).id().stripPrefix("sha256:")
        }
      )
    } catch {
      case NonFatal(e) =>
        if (Files.isDirectory(buildPath)) {
          FileUtils.deleteDirectory(buildPath.toFile)
        }
        Result.internalErrorF(e)
    }
  }

  private def prepareScript(modelBuild: ModelBuild, script: String) = {
    script
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_PATH + "\\}", modelRootDir)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_VERSION + "\\}", modelBuild.modelVersion.toString)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_NAME + "\\}", modelBuild.model.name)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_TYPE + "\\}", modelBuild.model.modelType.toTag)
  }

}