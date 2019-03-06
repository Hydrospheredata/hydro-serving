package io.hydrosphere.serving.manager.domain.clouddriver

import cats._
import cats.effect.Sync
import cats.implicits._
import com.spotify.docker.client.DockerClient.{ListContainersParam, RemoveContainerParam}
import com.spotify.docker.client.messages._
import io.hydrosphere.serving.manager.config.CloudDriverConfiguration
import io.hydrosphere.serving.manager.domain.servable.Servable

import scala.collection.JavaConverters._

class DockerDriver2[F[_]](
  client: DockerdClient[F],
  config: CloudDriverConfiguration.Docker)(
  implicit F: MonadError[F, Throwable]
) extends CloudDriver2[F] {
  
  import DockerDriver2._
  
  override def instances: F[List[ServingInstance]] = {
    client.listContainers.map(all => {
      all.map(containerToInstance).collect({case Some(v) => v})
    })
  }
  
  override def instance(id: String): F[Option[ServingInstance]] = {
    val query = List(ListContainersParam.withLabel(DefaultConstants.LABEL_SERVICE_ID, id.toString))
    client.listContainers(query).map(r => {
      r.headOption.flatMap(containerToInstance)
    })
  }
  
  override def run(servable: Servable): F[ServingInstance] = {
    val container = Internals.mkContainerConfig(servable, config)
    for {
      creation <- client.createContainer(container, servable.serviceName.some)
      maybeOut <- instance(servable.id.toString)
      out      <- maybeOut match {
        case Some(v) => F.pure(v)
        case None =>
          val warnings = creation.warnings().asScala.mkString("\n")
          val msg = s"Running docker container for ${servable.id} failed. Reason: \n $warnings"
          F.raiseError(new RuntimeException(msg))
      }
    } yield out
  }
  
  override def remove(id: String): F[Unit] = {
    val params = List(
      RemoveContainerParam.forceKill(true),
      RemoveContainerParam.removeVolumes(true)
    )
    client.removeContainer(id, params)
  }
  
  private def containerToInstance(c: Container): Option[ServingInstance] = {
    val labels = c.labels().asScala
  
    val mId = labels.get(Labels.ServiceId)
    val mName = labels.get(Labels.ServiceName)
  
    (mId, mName).mapN((id, name) => {
      val host = Internals.extractIpAddress(c.networkSettings(), config.networkName)
      ServingInstance(id, host, DefaultConstants.DEFAULT_APP_PORT)
    })
  }
  
  
}

object DockerDriver2 {
  
  object Labels {
    val ServiceName = "HS_INSTANCE_NAME"
    val ServiceId = "HS_INSTANCE_ID"
  }
  
  object Internals {
    
    def mkContainerConfig(
      servable: Servable,
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
        Labels.ServiceName -> servable.serviceName,
        Labels.ServiceId -> servable.id.toString
      )
      val envMap = servable.configParams ++ Map(
        DefaultConstants.ENV_MODEL_DIR -> DefaultConstants.DEFAULT_MODEL_DIR.toString,
        DefaultConstants.ENV_APP_PORT -> DefaultConstants.DEFAULT_APP_PORT.toString,
        DefaultConstants.LABEL_SERVICE_ID -> servable.id.toString
      )
  
      val envs = envMap.map({ case (k, v) => s"$k=$v"}).toList.asJava
  
      ContainerConfig.builder()
        .image(servable.modelVersion.image.fullName)
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
