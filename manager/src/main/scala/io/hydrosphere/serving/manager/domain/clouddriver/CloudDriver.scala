package io.hydrosphere.serving.manager.domain.clouddriver

import akka.actor.{ActorRef, ActorSystem, Props}
import cats.effect.{Async, Effect}
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ecs.AmazonECSClientBuilder
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder
import com.spotify.docker.client.DockerClient
import io.hydrosphere.serving.manager.config._
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.servable.Servable
import io.hydrosphere.serving.manager.infrastructure.clouddriver._
import io.hydrosphere.serving.manager.infrastructure.envoy.events.{CloudServiceDiscoveryEventBus, DiscoveryEventBus}

import scala.concurrent.ExecutionContext


trait CloudDriver[F[_]] {

  def serviceList(): F[Seq[CloudService]]

  def deployService(
    service: Servable,
    modelVersion: DockerImage,
    hostSelector: Option[HostSelector]
  ): F[CloudService]

  def removeService(serviceId: Long): F[Unit]
}

object CloudDriver {
  def fromConfig[F[_] : Effect](
    dockerClient: DockerClient,
    eventPublisher: CloudServiceDiscoveryEventBus[F],
    cloudDriverConfiguration: CloudDriverConfiguration,
    applicationConfiguration: ApplicationConfig,
    advertisedConfiguration: AdvertisedConfiguration,
    dockerRepositoryConfiguration: DockerRepositoryConfiguration,
    sidecarConfig: SidecarConfig
  )(implicit executionContext: ExecutionContext, actorSystem: ActorSystem): CloudDriver[F] = {
    cloudDriverConfiguration match {
      case x: CloudDriverConfiguration.Ecs =>
        val route53Client = AmazonRoute53ClientBuilder.standard()
          .build()
        val ecsClient = AmazonECSClientBuilder.standard()
          .withRegion(x.region)
          .build()
        val ec2Client = AmazonEC2ClientBuilder.standard()
          .withRegion(x.region)
          .build()
        val ecsSyncActor: ActorRef = actorSystem.actorOf(ECSWatcherActor.props[F](eventPublisher, x, route53Client, ecsClient, ec2Client))
        new ECSCloudDriver(x, ecsSyncActor)
      case x: CloudDriverConfiguration.Docker => new DockerComposeCloudDriver(dockerClient, x, applicationConfiguration, sidecarConfig, advertisedConfiguration, eventPublisher)
      case x: CloudDriverConfiguration.Kubernetes =>
        dockerRepositoryConfiguration match {
          case repo: DockerRepositoryConfiguration.Remote => new KubernetesCloudDriver(x, repo, eventPublisher)
          case repo => throw new IllegalArgumentException(s"Illegal DockerRepositoryConfiguration for Kubernetes clouddriver: $repo")
        }
      case x: CloudDriverConfiguration.Local => new LocalDockerCloudDriver(dockerClient, applicationConfiguration, sidecarConfig, advertisedConfiguration, x, eventPublisher)
      case x => throw new IllegalArgumentException(s"Unknown CloudDriverConfiguration: $x")
    }

  }
}