package io.hydrosphere.serving.manager.domain.servable

import cats.effect._
import cats.implicits._
import io.hydrosphere.serving.manager.domain.clouddriver._
import io.hydrosphere.serving.manager.domain.image.DockerImage
import org.apache.logging.log4j.scala.Logging

import scala.util.control.NonFatal

trait ServableService[F[_]] {
  def stop(name: String): F[Unit]
  def deploy(name: String, modelVersionId: Long, image: DockerImage): F[Servable]
}

object ServableService {
  
  def apply[F[_]](
    cloudDriver: CloudDriver[F]
  )(implicit F: Sync[F]): ServableService[F] =
    new ServableService[F] with Logging {
  
      override def deploy(name: String, modelVersionId: Long, image: DockerImage): F[Servable] = {
        cloudDriver.run(name, modelVersionId, image).onError {
          case NonFatal(ex) =>
            Sync[F].delay(logger.error(ex))
        }
      }
      
      override def stop(name: String): F[Unit] = cloudDriver.remove(name)

  }
}