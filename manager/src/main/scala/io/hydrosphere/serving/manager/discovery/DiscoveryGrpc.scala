package io.hydrosphere.serving.manager.discovery

import java.util.UUID

import cats.effect.Effect
import com.google.protobuf.empty.Empty
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.discovery.serving.ServingDiscoveryGrpc.ServingDiscovery
import io.hydrosphere.serving.discovery.serving._
import org.apache.logging.log4j.scala.Logging


object DiscoveryGrpc {
  
  class GrpcServingDiscovery[F[_]](
    discoveryHub: ObservedDiscoveryHub[F])(
    implicit F: Effect[F]
  ) extends ServingDiscovery with Logging {
  
    private def runSync[A](f: => F[A]): A = F.toIO(f).unsafeRunSync()
    
    override def watch(observer: StreamObserver[WatchResp]): StreamObserver[Empty] = {
      val id = UUID.randomUUID().toString
      
      val obs = AppsObserver.grpc[F](observer)
      runSync(discoveryHub.register(id, obs))
      
      new StreamObserver[Empty] {
        
        override def onNext(value: Empty): Unit = ()
        
        override def onError(t: Throwable): Unit = {
          logger.error("Client stream failed", t)
          runSync(discoveryHub.unregister(id))
        }
        
        override def onCompleted(): Unit =
          runSync(discoveryHub.unregister(id))
      }
    }
  }
  
}
