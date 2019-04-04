package io.hydrosphere.serving.manager.domain.clouddriver

import cats._
import cats.implicits._
import com.spotify.docker.client.DockerClient.{ListContainersParam, RemoveContainerParam}
import com.spotify.docker.client.messages._
import io.hydrosphere.serving.manager.config.CloudDriverConfiguration
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.servable.{Servable, ServableStatus}

import scala.collection.JavaConverters._
import scala.util.Try

class DockerDriver[F[_]](
  client: DockerdClient[F],
  config: CloudDriverConfiguration.Docker)(
  implicit F: MonadError[F, Throwable]
) extends CloudDriver[F] {
  
  import DockerDriver._
  
  override def instances: F[List[Servable]] = {
    client.listContainers.map(all => {
      all.map(containerToInstance).collect({ case Some(v) => v })
    })
  }
  
  private def containerOf(name: String): F[Option[Container]] = {
    val query = List(
      ListContainersParam.withLabel(CloudDriver.Labels.ServiceName, name)
    )
    client.listContainers(query).map(_.headOption)
  }
  
  override def instance(name: String): F[Option[Servable]] =
    containerOf(name).map(_.flatMap(containerToInstance))
  
  override def run(
    name: String,
    modelVersionId: Long,
    image: DockerImage
  ): F[Servable] = {
    val container = Internals.mkContainerConfig(name, modelVersionId, image, config)
    for {
      creation <- client.createContainer(container, None)
      _        <- client.runContainer(creation.id())
      maybeOut <- instance(name)
      out      <- maybeOut match {
        case Some(v) => F.pure(v)
        case None =>
          val warnings = Option(creation.warnings()) match {
            case Some(l) => l.asScala.mkString("\n")
            case None => ""
          }
          val msg = s"Running docker container for $name (${creation.id()}) failed. Warnings: \n $warnings"
          F.raiseError(new RuntimeException(msg))
      }
    } yield out
  }
  
  override def remove(name: String): F[Unit] = {
    for {
      maybeC  <- containerOf(name)
      _       <- maybeC match {
        case Some(c) =>
          val params = List(
            RemoveContainerParam.forceKill(true),
            RemoveContainerParam.removeVolumes(true),
          )
          client.removeContainer(c.id, params)
        case None => F.raiseError(new Exception(s"Could not find container for $name"))
      }
    } yield ()
  }
  
  private def containerToInstance(c: Container): Option[Servable] = {
    val labels = c.labels().asScala
  
    val mName = labels.get(CloudDriver.Labels.ServiceName)
    val mMvId = labels.get(CloudDriver.Labels.ModelVersionId).flatMap(i => Try(i.toLong).toOption)
  
    (mName, mMvId).mapN((name, mvId) => {
      val host = Internals.extractIpAddress(c.networkSettings(), config.networkName)
      val status = ServableStatus.Running(host, DefaultConstants.DEFAULT_APP_PORT)
      Servable(mvId, name, status)
    })
  }
  
  
}

object DockerDriver {
  
  
  object Internals {
  
    def mkContainerConfig(
      name: String,
      modelVersionId: Long,
      image: DockerImage,
      dockerConf: CloudDriverConfiguration.Docker
    ): ContainerConfig = {
      val hostConfig = {
        val builder = HostConfig.builder().networkMode(dockerConf.networkName)
        val withLogs = dockerConf.loggingConfiguration match {
          case Some(c) => builder.logConfig(LogConfig.create(c.driver, c.params.asJava))
          case None => builder
        }
        withLogs.build()
      }
  
      val labels = Map(
        CloudDriver.Labels.ServiceName -> name,
        CloudDriver.Labels.ModelVersionId -> modelVersionId.toString
      )
      val envMap = Map(
        DefaultConstants.ENV_MODEL_DIR -> DefaultConstants.DEFAULT_MODEL_DIR.toString,
        DefaultConstants.ENV_APP_PORT -> DefaultConstants.DEFAULT_APP_PORT.toString
      )
  
      val envs = envMap.map({ case (k, v) => s"$k=$v"}).toList.asJava
  
      ContainerConfig.builder()
        .image(image.fullName)
        .exposedPorts(DefaultConstants.DEFAULT_APP_PORT.toString)
        .labels(labels.asJava)
        .hostConfig(hostConfig)
        .env(envs)
        .build()
    }
  
    def extractIpAddress(settings: NetworkSettings, networkName: String): String = {
      val byNetworkName = Option(settings.networks().get(networkName)).map(_.ipAddress())
      byNetworkName.getOrElse(settings.ipAddress())
    }
  }
  
  
}
