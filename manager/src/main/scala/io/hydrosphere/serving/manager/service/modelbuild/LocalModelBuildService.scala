package io.hydrosphere.serving.manager.service.modelbuild

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path}

import cats.data.EitherT
import cats.implicits._
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.BuildParam
import com.spotify.docker.client.exceptions.DockerException
import io.hydrosphere.serving.manager.model.{HFResult, ModelBuild, Result}
import io.hydrosphere.serving.manager.service.SourceManagementService
import org.apache.commons.io.FileUtils
import io.hydrosphere.serving.manager.model.Result.Implicits._
import sun.plugin.dom.exception.InvalidStateException

import scala.concurrent.{ExecutionContext, Future}

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
      localPath <- EitherT(sourceManagementService.getLocalPath(modelBuild.model.source))
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
      }.toHResult(Result.InternalError(new InvalidStateException("Can't build docker container")))

      Future.successful(
        dockerContainer.right.map { container =>
          dockerClient.inspectImage(container).id().stripPrefix("sha256:")
        }
      )
    } catch {
      case dEx: DockerException =>
        Files.deleteIfExists(buildPath)
        Result.internalErrorF(dEx)
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