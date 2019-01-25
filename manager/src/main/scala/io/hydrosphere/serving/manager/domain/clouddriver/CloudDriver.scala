package io.hydrosphere.serving.manager.domain.clouddriver

import akka.actor.{ActorRef, ActorSystem, Props}
import cats.effect.Async
import com.spotify.docker.client.DockerClient
import io.hydrosphere.serving.manager.config._
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.servable.Servable
import io.hydrosphere.serving.manager.infrastructure.clouddriver._
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.ManagerEventBus

import scala.concurrent.ExecutionContext


trait CloudDriver[F[_]] {

  def serviceList(): F[Seq[CloudService]]

  def deployService(
    service: Servable,
    runtime: DockerImage,
    modelVersion: DockerImage,
    hostSelector: Option[HostSelector]
  ): F[CloudService]

  def removeService(serviceId: Long): F[Unit]
}

object CloudDriver {
  def fromConfig[F[_]: Async](
    dockerClient: DockerClient,
    eventPublisher: ManagerEventBus[F],
    cloudDriverConfiguration: CloudDriverConfiguration,
    applicationConfiguration: ApplicationConfig,
    advertisedConfiguration: AdvertisedConfiguration,
    dockerRepositoryConfiguration: DockerRepositoryConfiguration,
    sidecarConfig: SidecarConfig
  )(implicit executionContext: ExecutionContext, actorSystem: ActorSystem): CloudDriver[F] = {
    cloudDriverConfiguration match {
      case x: CloudDriverConfiguration.Ecs =>
        val ecsSyncActor: ActorRef = actorSystem.actorOf(Props(classOf[ECSServiceWatcherActor[F]], eventPublisher, x))
        new ECSCloudDriverService(x, ecsSyncActor)
      case x: CloudDriverConfiguration.Docker => new DockerComposeCloudDriverService(dockerClient, x, applicationConfiguration, sidecarConfig, advertisedConfiguration, eventPublisher)
      case x: CloudDriverConfiguration.Kubernetes =>
        dockerRepositoryConfiguration match {
          case repo: DockerRepositoryConfiguration.Remote => new KubernetesCloudDriverService(x, repo, eventPublisher)
          case repo => throw new IllegalArgumentException(s"Illegal DockerRepositoryConfiguration for Kubernetes clouddriver: $repo")
        }
      case x: CloudDriverConfiguration.Local => new LocalCloudDriverService(dockerClient, applicationConfiguration, sidecarConfig, advertisedConfiguration, x, eventPublisher)
      case x => throw new IllegalArgumentException(s"Unknown CloudDriverConfiguration: $x")
    }

  }
}