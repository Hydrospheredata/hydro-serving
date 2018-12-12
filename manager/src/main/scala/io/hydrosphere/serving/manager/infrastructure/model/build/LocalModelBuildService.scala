package io.hydrosphere.serving.manager.infrastructure.model.build

import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.nio.file.{Files, Path}

import cats.data.EitherT
import cats.implicits._
import com.spotify.docker.client.DockerClient.BuildParam
import com.spotify.docker.client.{DockerClient, ProgressHandler}
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.config.DockerClientConfig
import io.hydrosphere.serving.manager.infrastructure.protocol.CommonJsonProtocol._
import io.hydrosphere.serving.manager.infrastructure.storage.ModelStorageService
import io.hydrosphere.serving.manager.util.ReflectionUtils
import io.hydrosphere.serving.model.api.Result.Implicits._
import io.hydrosphere.serving.model.api.{HFResult, ModelType, Result}
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import io.hydrosphere.serving.manager.domain.build_script.BuildScriptVariables._
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model_version.ModelBuildAlgebra

class LocalModelBuildService(
  dockerClient: DockerClient,
  dockerClientConfig: DockerClientConfig,
  sourceManagementService: ModelStorageService
)(implicit val ex: ExecutionContext) extends ModelBuildAlgebra with Logging {
  private val modelRootDir = "model"
  private val modelFilesDir = s"$modelRootDir/files/"
  private val contractFile = s"$modelRootDir/contract.protobin"

  private val buildParams: Seq[BuildParam] = {
    getBuildArgsParam :+ BuildParam.noCache()
  }

  override def build(
    modelName: String,
    modelVersion: Long,
    modelType: ModelType,
    contract: ModelContract,
    image: DockerImage,
    script: String,
    progressHandler: ProgressHandler
  ): HFResult[String] = {
    val tmpBuildPath = Files.createTempDirectory(s"hydroserving-$modelName-$modelVersion")
    val fT = for {
      localPath <- EitherT(sourceManagementService.getLocalPath(modelName))
      dockerFile = prepareScript(modelName, modelVersion, modelType, script)
      buildRes <- EitherT(build(tmpBuildPath, localPath, dockerFile, progressHandler, image, contract))
    } yield buildRes
    fT.value
  }

  private def getBuildArgsParam: Seq[BuildParam] = {
    val proxies = dockerClientConfig.proxies
    val proxyConfigs = proxies.get(dockerClient.getHost) orElse proxies.get("default")
    proxyConfigs.map { config =>
      val paramMap = Seq(
        config.httpProxy.map(x => "HTTP_PROXY" -> x),
        config.httpsProxy.map(x => "HTTPS_PROXY" -> x),
        config.noProxy.map(x => "NO_PROXY" -> x),
        config.ftpProxy.map(x => "FTP_PROXY" -> x),
      ).flatten.toMap
      BuildParam.create("buildargs", URLEncoder.encode(paramMap.toJson.compactPrint, "UTF-8"))
    }.toSeq
  }

  private def build(
    buildPath: Path,
    model: Path,
    dockerFile: String,
    progressHandler: ProgressHandler,
    image: DockerImage,
    contract: ModelContract): HFResult[String] = {
    try {
      Files.copy(new ByteArrayInputStream(dockerFile.getBytes), buildPath.resolve("Dockerfile"))
      Files.createDirectories(buildPath.resolve(modelRootDir))
      FileUtils.copyDirectory(model.toFile, buildPath.resolve(modelFilesDir).toFile)
      Files.write(buildPath.resolve(contractFile), contract.toByteArray)
      logger.debug(s"Sending docker build request with ${ReflectionUtils.prettyPrint(buildParams)} params")
      val dockerContainer = Option {
        dockerClient.build(
          buildPath,
          image.fullName,
          "Dockerfile",
          progressHandler,
          buildParams: _*
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

  private def prepareScript(modelName: String, modelVersion: Long, modelType: ModelType, script: String) = {
    script
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_PATH + "\\}", modelRootDir)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_VERSION + "\\}", modelVersion.toString)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_NAME + "\\}", modelName)
      .replaceAll("\\{" + SCRIPT_VAL_MODEL_TYPE + "\\}", modelType.toTag)
  }
}