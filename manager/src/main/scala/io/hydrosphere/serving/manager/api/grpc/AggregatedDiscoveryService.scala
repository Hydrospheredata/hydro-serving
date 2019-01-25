package io.hydrosphere.serving.manager.api.grpc

import cats.effect.Effect
import cats.syntax.functor._
import envoy.api.v2.{DiscoveryRequest, DiscoveryResponse}
import envoy.service.discovery.v2.AggregatedDiscoveryServiceGrpc
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.infrastructure.envoy.EnvoyGRPCDiscoveryService
import org.apache.logging.log4j.scala.Logging

class AggregatedDiscoveryService[F[_]: Effect](
  envoyGRPCDiscoveryService: EnvoyGRPCDiscoveryService[F]
) extends AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryService {

  override def streamAggregatedResources(responseObserver: StreamObserver[DiscoveryResponse]): StreamObserver[DiscoveryRequest] = {
    AggregatedDiscoveryService.observer(envoyGRPCDiscoveryService, responseObserver)
  }
}

object AggregatedDiscoveryService {
  def observer[F[_] : Effect](
    envoyDiscovery: EnvoyGRPCDiscoveryService[F],
    responseObserver: StreamObserver[DiscoveryResponse]
  ): StreamObserver[DiscoveryRequest] =
    new StreamObserver[DiscoveryRequest] with Logging {
      override def onError(t: Throwable): Unit = Effect[F].toIO {
        envoyDiscovery.unsubscribe(responseObserver).map { _ =>
          logger.error(t.getMessage, t)
        }
      }.unsafeRunAsyncAndForget()

      override def onCompleted(): Unit = Effect[F].toIO {
        envoyDiscovery.unsubscribe(responseObserver).map { _ =>
          logger.debug(s"Discovery service stream completed")
        }
      }.unsafeRunAsyncAndForget()

      override def onNext(value: DiscoveryRequest): Unit = Effect[F].toIO {
        envoyDiscovery.subscribe(value, responseObserver).map { _ =>
          logger.debug(s"Discovery service stream got next element: $value")
        }
      }.unsafeRunAsyncAndForget()
    }
}