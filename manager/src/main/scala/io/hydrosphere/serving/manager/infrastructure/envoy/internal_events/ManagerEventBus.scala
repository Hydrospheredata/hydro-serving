package io.hydrosphere.serving.manager.infrastructure.envoy.internal_events

import akka.actor.ActorSystem
import cats.effect.Sync
import io.hydrosphere.serving.manager.domain.application.Application
import io.hydrosphere.serving.manager.domain.servable.Servable
import io.hydrosphere.serving.manager.domain.clouddriver.CloudService
import io.hydrosphere.serving.manager.service.internal_events._
import org.apache.logging.log4j.scala.Logging

trait ManagerEventBus[F[_]] {
  def applicationChanged(application: Application): F[Unit]

  def applicationRemoved(application: Application): F[Unit]

  def serviceChanged(service: Servable): F[Unit]

  def serviceRemoved(service: Servable): F[Unit]

  def cloudServiceDetected(cloudService: Seq[CloudService]): F[Unit]

  def cloudServiceRemoved(cloudService: Seq[CloudService]): F[Unit]
}

object ManagerEventBus {
  def fromActorSystem[F[_] : Sync](actorSystem: ActorSystem): ManagerEventBus[F] = new ManagerEventBus[F] with Logging {
    def applicationChanged(application: Application) = Sync[F].delay {
      logger.info(s"Application changed: $application")
      actorSystem.eventStream.publish(ApplicationChanged(application))
    }

    def applicationRemoved(application: Application) = Sync[F].delay {
      logger.info(s"Application removed: $application")
      actorSystem.eventStream.publish(ApplicationRemoved(application))
    }

    def serviceChanged(service: Servable) = Sync[F].delay {
      logger.info(s"Service changed: $service")
      actorSystem.eventStream.publish(ServiceChanged(service))
    }

    def serviceRemoved(service: Servable) = Sync[F].delay {
      logger.info(s"Service removed: $service")
      actorSystem.eventStream.publish(ServiceRemoved(service))
    }

    def cloudServiceDetected(cloudService: Seq[CloudService]) = Sync[F].delay {
      logger.info(s"Cloud service detected: $cloudService")
      actorSystem.eventStream.publish(CloudServiceDetected(cloudService))
    }

    def cloudServiceRemoved(cloudService: Seq[CloudService]) = Sync[F].delay {
      logger.info(s"Cloud service removed: $cloudService")
      actorSystem.eventStream.publish(CloudServiceRemoved(cloudService))
    }
  }
}