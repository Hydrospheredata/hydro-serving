package io.hydrosphere.serving.manager.discovery

import cats.effect.Sync
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.discovery.serving.WatchResp
import io.hydrosphere.serving.manager.grpc.entities.ServingApp

trait AppsObserver[F[_]] {
  def apps(apps: List[ServingApp]): F[Unit]
  def added(app: ServingApp): F[Unit]
  def removed(ids: List[Long]): F[Unit]
}

object AppsObserver {
  
  def grpc[F[_]](observer: StreamObserver[WatchResp])(implicit F: Sync[F]): AppsObserver[F] = {
    new AppsObserver[F] {
      
      override def apps(apps: List[ServingApp]): F[Unit] = {
        F.delay {
          val rsp = WatchResp(added = apps)
          observer.onNext(rsp)
        }
      }
      override def added(app: ServingApp): F[Unit] = {
        F.delay{
          val rsp = WatchResp(added = List(app))
          observer.onNext(rsp)
        }
      }
      override def removed(ids: List[Long]): F[Unit] = {
        F.delay {
          val rsp = WatchResp(ids.map(_.toString))
          observer.onNext(rsp)
        }
      }
      
    }
  }
  
}

