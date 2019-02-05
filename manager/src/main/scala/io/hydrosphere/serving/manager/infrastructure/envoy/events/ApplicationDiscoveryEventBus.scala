package io.hydrosphere.serving.manager.infrastructure.envoy.events

import akka.actor.ActorSystem
import cats.effect.Sync
import io.hydrosphere.serving.manager.domain.application.Application
import io.hydrosphere.serving.manager.infrastructure.envoy.events.DiscoveryEvent._
import org.apache.logging.log4j.scala.Logging

trait ApplicationDiscoveryEventBus[F[_]] extends DiscoveryEventBus[F, Application]

object ApplicationDiscoveryEventBus {
  def fromActorSystem[F[_] : Sync](actorSystem: ActorSystem): ApplicationDiscoveryEventBus[F] = new ApplicationDiscoveryEventBus[F] with Logging {

    override def detected(objs: Application): F[Unit] = Sync[F].delay {
      logger.info(s"Application changed: $objs")
      actorSystem.eventStream.publish(ApplicationChanged(objs))
    }

    override def removed(objs: Application): F[Unit] = Sync[F].delay {
      logger.info(s"Application removed: $objs")
      actorSystem.eventStream.publish(ApplicationRemoved(objs))
    }
  }
}