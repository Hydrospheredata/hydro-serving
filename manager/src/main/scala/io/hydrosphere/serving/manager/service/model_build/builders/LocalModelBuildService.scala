package io.hydrosphere.serving.manager.service.model_build.builders

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path}

import cats.data.EitherT
import cats.implicits._
import com.spotify.docker.client.DockerClient.BuildParam
import com.spotify.docker.client.{DockerClient, ProgressHandler}
import io.hydrosphere.serving.manager.config.DockerClientConfig
import io.hydrosphere.serving.manager.model.db.ModelBuild
import io.hydrosphere.serving.manager.service.source.ModelStorageService
import io.hydrosphere.serving.manager.util.ReflectionUtils
import io.hydrosphere.serving.model.api.Result.Implicits._
import io.hydrosphere.serving.model.api.{HFResult, Result}
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.scala.Logging

import java.net.URLEncoder

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import spray.json._
import io.hydrosphere.serving.manager.model.protocol.CommonJsonProtocol._

class LocalModelBuildService(
  dockerClient: DockerClient,
  dockerClientConfig: DockerClientConfig,
  sourceManagementService: ModelStorageService
)(implicit val ex: ExecutionContext) extends ModelBuildService with Logging {
  private val modelRootDir = "model"
  private val modelFilesDir = s"$modelRootDir/files/"
  private val contractFile = s"$modelRootDir/contract.protobin"

  private val buildParams: Seq[BuildParam] = {
    getBuildArgsParam :+ BuildParam.noCache()
  }

  override def build(modelBuild: ModelBuild, imageName: String, script: String, progressHandler: ProgressHandler): HFResult[String] = {
    val tmpBuildPath = Files.createTempDirectory(s"hydroserving-${modelBuild.id}")
    val fT = for {
      localPath <- EitherT(sourceManagementService.getLocalPath(modelBuild.model.name))
      dockerFile = prepareScript(modelBuild, script)
      buildRes <- EitherT(build(tmpBuildPath, localPath, dockerFile, progressHandler, modelBuild, imageName))
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

  private def build(buildPath: Path, model: Path, dockerFile: String, progressHandler: ProgressHandler, modelBuild: ModelBuild, imageName: String): HFResult[String] = {
    try {
      Files.copy(new ByteArrayInputStream(dockerFile.getBytes), buildPath.resolve("Dockerfile"))
      Files.createDirectories(buildPath.resolve(modelRootDir))
      FileUtils.copyDirectory(model.toFile, buildPath.resolve(modelFilesDir).toFile)
      Files.write(buildPath.resolve(contractFile), modelBuild.model.modelContract.toByteArray)
      logger.debug(s"Sending docker build request with ${ReflectionUtils.prettyPrint(buildParams)} params")
      val dockerContainer = Option {
        dockerClient.build(
          buildPath,
          imageName,
          "Dockerfile",
          progressHandler,
          buildParams :_*
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