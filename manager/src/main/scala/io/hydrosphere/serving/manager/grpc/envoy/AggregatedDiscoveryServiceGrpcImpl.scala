package io.hydrosphere.serving.manager.grpc.envoy

import envoy.api.v2.{AggregatedDiscoveryServiceGrpc, DiscoveryRequest, DiscoveryResponse}
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.service.envoy.EnvoyDiscoveryService
import org.apache.logging.log4j.scala.Logging

/**
  *
  */
class AggregatedDiscoveryServiceGrpcImpl(
  envoyDiscoveryService: EnvoyDiscoveryService
) extends AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryService with Logging {

  override def streamAggregatedResources(responseObserver: StreamObserver[DiscoveryResponse]): StreamObserver[DiscoveryRequest] = {
    new StreamObserver[DiscoveryRequest] {

      override def onError(t: Throwable): Unit = {
        logger.error(t.getMessage, t)
        envoyDiscoveryService.unsubscribe(responseObserver)
      }

      override def onCompleted(): Unit = {
        envoyDiscoveryService.unsubscribe(responseObserver)
      }

      override def onNext(value: DiscoveryRequest): Unit =
        envoyDiscoveryService.subscribe(value, responseObserver)
    }
  }
}