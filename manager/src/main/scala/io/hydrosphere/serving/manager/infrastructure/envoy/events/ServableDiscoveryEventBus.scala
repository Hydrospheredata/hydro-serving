package io.hydrosphere.serving.manager.infrastructure.envoy.events

import akka.actor.ActorSystem
import cats.effect.Sync
import io.hydrosphere.serving.manager.domain.servable.Servable
import io.hydrosphere.serving.manager.infrastructure.envoy.events.DiscoveryEvent._
import org.apache.logging.log4j.scala.Logging

trait ServableDiscoveryEventBus[F[_]] extends DiscoveryEventBus[F, Servable]

object ServableDiscoveryEventBus {
  def fromActorSystem[F[_] : Sync](actorSystem: ActorSystem): ServableDiscoveryEventBus[F] = new ServableDiscoveryEventBus[F] with Logging {

    override def detected(objs: Servable): F[Unit] = Sync[F].delay {
      logger.info(s"Service changed: $objs")
      actorSystem.eventStream.publish(ServiceChanged(objs))
    }

    override def removed(objs: Servable): F[Unit] = Sync[F].delay {
      logger.info(s"Service removed: $objs")
      actorSystem.eventStream.publish(ServiceRemoved(objs))
    }
  }
}