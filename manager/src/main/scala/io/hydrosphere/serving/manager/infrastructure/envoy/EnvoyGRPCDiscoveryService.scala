package io.hydrosphere.serving.manager.infrastructure.envoy

import akka.actor.ActorRef
import cats.effect.Sync
import envoy.api.v2.{DiscoveryRequest, DiscoveryResponse}
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.domain.application.ApplicationService
import io.hydrosphere.serving.manager.domain.servable.ServableService
import io.hydrosphere.serving.manager.infrastructure.envoy.xds._

trait EnvoyGRPCDiscoveryService[F[_]] {
  def subscribe(discoveryRequest: DiscoveryRequest, responseObserver: StreamObserver[DiscoveryResponse]): F[Unit]

  def unsubscribe(responseObserver: StreamObserver[DiscoveryResponse]): F[Unit]
}

object EnvoyGRPCDiscoveryService {
  def actorManaged[F[_] : Sync](
    xdsActor: ActorRef,
    servableService: ServableService[F],
    appService: ApplicationService[F],
  ) = new EnvoyGRPCDiscoveryService[F] {
    override def subscribe(
      discoveryRequest: DiscoveryRequest,
      responseObserver: StreamObserver[DiscoveryResponse]
    ): F[Unit] = Sync[F].delay {
      discoveryRequest.node.foreach { _ =>
        xdsActor ! SubscribeMsg(
          discoveryRequest = discoveryRequest,
          responseObserver = responseObserver
        )
      }
    }

    override def unsubscribe(responseObserver: StreamObserver[DiscoveryResponse]): F[Unit] = Sync[F].delay {
      val msg = UnsubscribeMsg(responseObserver)
      xdsActor ! msg
    }
  }
}