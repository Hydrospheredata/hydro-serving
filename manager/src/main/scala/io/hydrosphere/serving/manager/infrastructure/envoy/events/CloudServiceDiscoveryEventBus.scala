package io.hydrosphere.serving.manager.infrastructure.envoy.events

import akka.actor.ActorSystem
import cats.effect.Sync
import io.hydrosphere.serving.manager.domain.clouddriver.CloudService
import io.hydrosphere.serving.manager.infrastructure.envoy.events.DiscoveryEvent._
import org.apache.logging.log4j.scala.Logging

trait CloudServiceDiscoveryEventBus[F[_]] extends DiscoveryEventBus[F, CloudService]

object CloudServiceDiscoveryEventBus {
  def fromActorSystem[F[_] : Sync](actorSystem: ActorSystem): CloudServiceDiscoveryEventBus[F] = new CloudServiceDiscoveryEventBus[F] with Logging {
    override def detected(objs: CloudService): F[Unit] = Sync[F].delay {
      logger.info(s"Cloud service detected: $objs")
      actorSystem.eventStream.publish(CloudServiceDetected(Seq(objs)))
    }

    override def removed(objs: CloudService): F[Unit] = Sync[F].delay {
      logger.info(s"Cloud service removed: $objs")
      actorSystem.eventStream.publish(CloudServiceRemoved(Seq(objs)))
    }
  }
}