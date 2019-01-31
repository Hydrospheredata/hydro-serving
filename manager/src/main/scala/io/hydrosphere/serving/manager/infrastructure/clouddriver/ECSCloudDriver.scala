package io.hydrosphere.serving.manager.infrastructure.clouddriver

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import cats.effect.Async
import io.hydrosphere.serving.manager.config.CloudDriverConfiguration
import io.hydrosphere.serving.manager.domain.clouddriver._
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.servable.Servable
import io.hydrosphere.serving.manager.util.AsyncUtil
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ECSCloudDriver[F[_]: Async](
  managerConfiguration: CloudDriverConfiguration.Ecs,
  ecsSyncActor: ActorRef
)(
  implicit val ex: ExecutionContext,
  actorSystem: ActorSystem
) extends CloudDriver[F] with Logging {
  implicit val timeout = Timeout(30.seconds)

  import ECSWatcherActor.Messages._

  override def removeService(serviceId: Long): F[Unit] = AsyncUtil.futureAsync {
    (ecsSyncActor ? RemoveService(serviceId)).mapTo[Unit]
  }

  override def deployService(service: Servable, modelVersion: DockerImage, host: Option[HostSelector]): F[CloudService] = AsyncUtil.futureAsync {
    (ecsSyncActor ? DeployService(service, modelVersion, host)).mapTo[CloudService]
  }

  override def serviceList(): F[Seq[CloudService]] = AsyncUtil.futureAsync {
    (ecsSyncActor ? ServiceList()).mapTo[Seq[CloudService]]
  }
}