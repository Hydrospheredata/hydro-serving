package io.hydrosphere.serving.manager.grpc.envoy

import envoy.api.v2.{AggregatedDiscoveryServiceGrpc, DiscoveryRequest, DiscoveryResponse}
import io.grpc.stub.StreamObserver
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  *
  */
class AggregatedDiscoveryServiceGrpcImpl
(implicit val ex: ExecutionContext)
  extends AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryService with Logging {

  override def streamAggregatedResources(responseObserver: StreamObserver[DiscoveryResponse]): StreamObserver[DiscoveryRequest] = {
    new StreamObserver[DiscoveryRequest] {

      override def onError(t: Throwable): Unit = logger.error(t.getMessage, t)

      override def onCompleted(): Unit = {
        logger.info("!!!!!")
      }

      override def onNext(value: DiscoveryRequest): Unit =
        getResource(value).onComplete {
          case Success(response) => responseObserver.onNext(response)
          case Failure(t) => logger.error(t.getMessage, t)
        }
    }
  }

  private def getResource(request: DiscoveryRequest): Future[DiscoveryResponse] = {
    logger.info(request)
    Future.failed(new RuntimeException)
  }
}