package io.hydrosphere.serving.manager.infrastructure.image

import java.net.URLEncoder
import java.nio.file.{Files, Path}

import cats.effect.Sync
import com.spotify.docker.client.DockerClient.BuildParam
import com.spotify.docker.client.{DockerClient, ProgressHandler}
import io.hydrosphere.serving.manager.config.DockerClientConfig
import io.hydrosphere.serving.manager.domain.image.{DockerImage, ImageBuilder}
import io.hydrosphere.serving.manager.infrastructure.protocol.CommonJsonProtocol._
import io.hydrosphere.serving.manager.infrastructure.storage.ModelStorage
import io.hydrosphere.serving.manager.util.ReflectionUtils
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.scala.Logging
import spray.json._

class DockerImageBuilder[F[_]: Sync](
  dockerClient: DockerClient,
  dockerClientConfig: DockerClientConfig,
  modelStorage: ModelStorage[F],
  progressHandler: ProgressHandler
) extends ImageBuilder[F] with Logging {

  private val buildParams: Seq[BuildParam] = {
    getBuildArgsParam :+ BuildParam.noCache()
  }

  override def build(
    buildPath: Path,
    image: DockerImage
  ): F[String] = {
    val buildF = Sync[F].delay {
      logger.debug(s"Sending docker build request with ${ReflectionUtils.prettyPrint(buildParams)} params")
      val dockerContainer = Option {
        dockerClient.build(
          buildPath,
          image.fullName,
          "Dockerfile",
          progressHandler,
          buildParams: _*
        )
      }.getOrElse(throw new RuntimeException("Can't build docker container"))

      dockerClient.inspectImage(dockerContainer).id().stripPrefix("sha256:")
    }

    Sync[F].onError(buildF) {
      case e =>
        Sync[F].delay {
          if (Files.isDirectory(buildPath)) {
            FileUtils.deleteDirectory(buildPath.toFile)
          }
          logger.error(e)
        }
    }
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
}