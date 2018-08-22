package io.hydrosphere.serving.manager.grpc.envoy

import envoy.api.v2.{DiscoveryRequest, DiscoveryResponse}
import envoy.service.discovery.v2.AggregatedDiscoveryServiceGrpc
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.service.envoy.EnvoyGRPCDiscoveryService
import org.apache.logging.log4j.scala.Logging

class AggregatedDiscoveryServiceGrpcImpl(
  envoyGRPCDiscoveryService: EnvoyGRPCDiscoveryService
) extends AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryService with Logging {

  override def streamAggregatedResources(responseObserver: StreamObserver[DiscoveryResponse]): StreamObserver[DiscoveryRequest] = {
    new StreamObserver[DiscoveryRequest] {
      override def onError(t: Throwable): Unit = {
        logger.error(t.getMessage, t)
        envoyGRPCDiscoveryService.unsubscribe(responseObserver)
      }

      override def onCompleted(): Unit = {
        logger.debug(s"Discovery service stream completed")
        envoyGRPCDiscoveryService.unsubscribe(responseObserver)
      }

      override def onNext(value: DiscoveryRequest): Unit = {
        logger.debug(s"Discovery service stream got next element: $value")
        envoyGRPCDiscoveryService.subscribe(value, responseObserver)
      }
    }
  }
}