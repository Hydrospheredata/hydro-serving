package io.hydrosphere.serving.manager.discovery

import java.util.UUID

import cats.effect.Effect
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.discovery.serving.ServingDiscoveryGrpc.ServingDiscovery
import io.hydrosphere.serving.discovery.serving._
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

object DiscoveryGrpc {
  
  def server[F[_]](
    discoveryHub: ObservedDiscoveryHub[F],
    port: Int
  )(implicit F: Effect[F]): io.grpc.Server = {
    val service = ServingDiscoveryGrpc.bindService(new GrpcServingDiscovery[F](discoveryHub), ExecutionContext.global)
    ServerBuilder.forPort(port).addService(service).build()
  }
  
  private class GrpcServingDiscovery[F[_]](
    discoveryHub: ObservedDiscoveryHub[F])(
    implicit F: Effect[F]
  ) extends ServingDiscovery with Logging {
    
    override def watch(observer: StreamObserver[WatchResp]): StreamObserver[WatchReq] = {
      val id = UUID.randomUUID().toString
      
      new StreamObserver[WatchReq] {
        
        def runSync[A](f: => F[A]): A = F.toIO(f).unsafeRunSync()
        
        override def onNext(value: WatchReq): Unit = {
          val obs = AppsObserver.grpc[F](observer)
          runSync(discoveryHub.register(id, obs))
        }
        
        override def onError(t: Throwable): Unit = {
          logger.error("Client stream failed", t)
          runSync(discoveryHub.unregister(id))
        }
        
        override def onCompleted(): Unit =
          runSync(discoveryHub.unregister(id))
      }
    }
    
    override def fetch(request: FetchReq): Future[FetchResp] = ???
  }
  
}
