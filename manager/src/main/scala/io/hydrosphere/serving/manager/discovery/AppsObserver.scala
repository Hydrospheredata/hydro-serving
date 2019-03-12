package io.hydrosphere.serving.manager.discovery

import cats.effect.Sync
import cats.syntax.option._
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.discovery.serving.{Servable, ServingApp, Stage, WatchResp}
import io.hydrosphere.serving.manager.domain.application.{Application, PipelineStage}

trait AppsObserver[F[_]] {
  def apps(apps: List[Application]): F[Unit]
  def added(app: Application): F[Unit]
  def removed(ids: List[Long]): F[Unit]
}

object AppsObserver {
  
  def grpc[F[_]](observer: StreamObserver[WatchResp])(implicit F: Sync[F]): AppsObserver[F] = {
    new AppsObserver[F] {
      
      override def apps(apps: List[Application]): F[Unit] = {
        F.delay {
          val rsp = WatchResp(added = apps.map(toServingApp))
          observer.onNext(rsp)
        }
      }
      override def added(app: Application): F[Unit] = {
        F.delay{
          val rsp = WatchResp(added = List(toServingApp(app)))
          observer.onNext(rsp)
        }
      }
      override def removed(ids: List[Long]): F[Unit] = {
        F.delay {
          val rsp = WatchResp(ids.map(_.toString))
          observer.onNext(rsp)
        }
      }
      
      def toServingApp(app: Application): ServingApp = {
        import app._
        
        val contract = ModelContract(
          modelName = app.name,
          signatures = Seq(app.signature)
        )
        val stages = app.executionGraph.stages.zipWithIndex.map({case (st, i) => {
          val servables = st.modelVariants.map(mv => Servable("localhost", 8080, mv.weight))
          val id = PipelineStage.stageId(app.id, i)
          Stage(id, st.signature.some, servables)
        }})
        
        ServingApp(
          app.id.toString,
          app.name,
          contract.some,
          stages
        )
      }
    }
  }
  
}

