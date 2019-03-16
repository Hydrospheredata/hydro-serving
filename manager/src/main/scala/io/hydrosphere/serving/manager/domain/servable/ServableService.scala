package io.hydrosphere.serving.manager.domain.servable

import cats._
import cats.data.OptionT
import cats.implicits._
import cats.effect._
import cats.effect.implicits._
import io.hydrosphere.serving.manager.domain.clouddriver._
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import org.apache.logging.log4j.scala.Logging

import scala.util.control.NonFatal

trait ServableService[F[_]] {
  def stop(serviceId: Long): F[Option[Servable]]
  def deploy(data: ServableData): F[Servable]
}

object ServableService {
  
  def apply[F[_]](
    cloudDriver: CloudDriver2[F],
    servableRepository: ServableRepository[F]
  )(implicit F: Sync[F]): ServableService[F] =
    new ServableService[F] with Logging {
  
      override def deploy(data: ServableData): F[Servable] = {
        val starting = Servable(data.id, data.modelVersion.id, data.serviceName, ServableStatus.Starting)
        val f = for {
          _        <- servableRepository.update(starting)
          instance <- cloudDriver.run(data)
          upd      <- servableRepository.update(instance)
        } yield upd
        
        f.onError {
          case NonFatal(ex) =>
            Sync[F].delay(logger.error(ex))
        }
      }
      
      override def stop(serviceId: Long): F[Option[Servable]] = {
        val f = for {
          srvbl   <- OptionT(servableRepository.get(serviceId))
          _       <- OptionT.liftF(cloudDriver.remove(srvbl.serviceName, srvbl.toString))
          stopped = srvbl.copy(status = ServableStatus.Stopped)
          out       <- OptionT.liftF(servableRepository.update(stopped))
        } yield out
        f.value
      }

  }
}