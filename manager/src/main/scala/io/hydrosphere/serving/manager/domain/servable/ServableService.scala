package io.hydrosphere.serving.manager.domain.servable

import cats.data.OptionT
import cats.effect._
import cats.implicits._
import io.hydrosphere.serving.manager.domain.clouddriver._
import io.hydrosphere.serving.manager.domain.image.DockerImage
import org.apache.logging.log4j.scala.Logging

import scala.util.control.NonFatal

trait ServableService[F[_]] {
  def stop(serviceId: Long): F[Option[Servable]]
  def deploy(name: String, modelVersionId: Long, image: DockerImage): F[Servable]
}

object ServableService {
  
  def apply[F[_]](
    cloudDriver: CloudDriver2[F],
    servableRepository: ServableRepository[F]
  )(implicit F: Sync[F]): ServableService[F] =
    new ServableService[F] with Logging {
  
      override def deploy(name: String, modelVersionId: Long, image: DockerImage): F[Servable] = {
        val starting = Servable(0L, modelVersionId, name, ServableStatus.Starting)
        val f = for {
          initial  <- servableRepository.update(starting)
          instance <- cloudDriver.run(initial.id, name, modelVersionId, image)
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
          stopped =  srvbl.copy(status = ServableStatus.Stopped)
          out     <- OptionT.liftF(servableRepository.update(stopped))
        } yield out
        f.value
      }

  }
}