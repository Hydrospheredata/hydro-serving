package io.hydrosphere.serving.manager.api.grpc

import cats._
import cats.implicits._
import cats.effect.concurrent.{Ref, Semaphore}
import cats.effect.{Effect, Sync}
import cats.syntax.functor._
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.discovery.serving.{FetchReq, FetchResp, WatchReq, WatchResp}
import io.hydrosphere.serving.discovery.serving.ServingDiscoveryGrpc.ServingDiscovery
import io.hydrosphere.serving.manager.domain.application.Application
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future

  
  
//class AggregatedDiscoveryService[F[_]: Effect](
//  envoyGRPCDiscoveryService: EnvoyGRPCDiscoveryService[F]
//) extends AggregatedDiscoveryServiceGrpc.AggregatedDiscoveryService {
//
//  override def streamAggregatedResources(responseObserver: StreamObserver[DiscoveryResponse]): StreamObserver[DiscoveryRequest] = {
//    AggregatedDiscoveryService.observer(envoyGRPCDiscoveryService, responseObserver)
//  }
//}
//
//object AggregatedDiscoveryService {
//  def observer[F[_] : Effect](
//    envoyDiscovery: EnvoyGRPCDiscoveryService[F],
//    responseObserver: StreamObserver[DiscoveryResponse]
//  ): StreamObserver[DiscoveryRequest] =
//    new StreamObserver[DiscoveryRequest] with Logging {
//      override def onError(t: Throwable): Unit = Effect[F].toIO {
//        envoyDiscovery.unsubscribe(responseObserver).map { _ =>
//          logger.error(t.getMessage, t)
//        }
//      }.unsafeRunAsyncAndForget()
//
//      override def onCompleted(): Unit = Effect[F].toIO {
//        envoyDiscovery.unsubscribe(responseObserver).map { _ =>
//          logger.debug(s"Discovery service stream completed")
//        }
//      }.unsafeRunAsyncAndForget()
//
//      override def onNext(value: DiscoveryRequest): Unit = Effect[F].toIO {
//        envoyDiscovery.subscribe(value, responseObserver).map { _ =>
//          logger.debug(s"Discovery service stream got next element: $value")
//        }
//      }.unsafeRunAsyncAndForget()
//    }
//}