package io.hydrosphere.serving.manager.domain.clouddriver

import akka.actor.ActorSystem
import akka.stream.Materializer
import cats.effect._
import io.hydrosphere.serving.manager.config.{CloudDriverConfiguration, DockerRepositoryConfiguration}
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.servable.Servable

import scala.concurrent.ExecutionContext

trait CloudDriver[F[_]] {
  
  def instances: F[List[Servable]]
  
  def instance(name: String): F[Option[Servable]]
  
  def run(name: String, modelVersionId: Long, image: DockerImage): F[Servable]
  
  def remove(name: String): F[Unit]
}

object CloudDriver {

  object Labels {
    val ServiceName = "HS_INSTANCE_NAME"
    val ModelVersionId = "HS_INSTANCE_MV_ID"
    val ServiceId = "HS_INSTANCE_ID"
  }

  def fromConfig[F[_]: Async](config: CloudDriverConfiguration, dockerRepoConf: DockerRepositoryConfiguration)(implicit ex: ExecutionContext, actorSystem: ActorSystem, materializer: Materializer): CloudDriver[F] = {
     config match {
       case dockerConf: CloudDriverConfiguration.Docker =>
         val client = DockerdClient.create
         new DockerDriver[F](client, dockerConf)
       case kubeConf: CloudDriverConfiguration.Kubernetes =>
         dockerRepoConf match {
           case drc: DockerRepositoryConfiguration.Remote =>
             val client = KubernetesClient[F](kubeConf, drc)
             new KubernetesDriver[F](client)
           case _ => throw new Exception(s"Docker Repository must be remote for using kubernetes cloud driver")
         }
       case x =>
         throw new Exception(s"Not implemented for $x")
     }
  }
}
