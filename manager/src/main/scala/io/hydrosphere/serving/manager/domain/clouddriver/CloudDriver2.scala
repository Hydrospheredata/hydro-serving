package io.hydrosphere.serving.manager.domain.clouddriver

import cats.effect.Sync
import io.hydrosphere.serving.manager.config.CloudDriverConfiguration
import io.hydrosphere.serving.manager.domain.servable.{Servable, ServableData}

final case class ServingInstance(
  name: String,
  id: String,
  host: String,
  port: Int
)

trait CloudDriver2[F[_]] {
  
  def instances: F[List[Servable]]
  
  def instance(name: String, id: String): F[Option[Servable]]
  
  def run(servable: ServableData): F[Servable]
  
  def remove(name: String, id: String): F[Unit]
}

object CloudDriver2 {
  
  def fromConfig[F[_]](config: CloudDriverConfiguration)(implicit F: Sync[F]): CloudDriver2[F] = {
     config match {
       case dockerConf: CloudDriverConfiguration.Docker =>
         val client = DockerdClient.create
         new DockerDriver2[F](client, dockerConf)
       case x =>
         throw new Exception(s"Not implemented for $x")
     }
  }
}
