package io.hydrosphere.serving.manager.domain.clouddriver
import cats.effect.Async
import cats.implicits._
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.servable.{Servable, ServableStatus}

import scala.util.Try

class KubernetesDriver[F[_]: Async](client: KubernetesClient[F]) extends CloudDriver[F] {
  private def kubeSvc2Servable(svc: skuber.Service): Option[Servable] = for {
    modelVersionId <- Try(svc.metadata.labels(CloudDriver.Labels.ModelVersionId).toLong).toOption
    serviceName <- Try(svc.metadata.labels(CloudDriver.Labels.ServiceName)).toOption
    host <- svc.spec.map(_.clusterIP)
    port <- svc.spec.flatMap(_.ports.find(_.name == "grpc")).map(_.port)
  } yield Servable(
    modelVersionId,
    serviceName,
    ServableStatus.Running(host, port)
  )
  
  override def instances: F[List[Servable]] = client.services.map(_.map(kubeSvc2Servable).collect {case Some(v) => v })

  override def instance(name: String): F[Option[Servable]] = instances.map(_.find(_.serviceName == name))

  override def run(name: String, modelVersionId: Long, image: DockerImage): F[Servable] = {
    val servable = Servable(modelVersionId, name, ServableStatus.Starting)
    for {
      _ <- client.runDeployment(name, servable, image)
      service <- client.runService(name, servable)
      maybeServable = kubeSvc2Servable(service)
      newServable <- maybeServable match {
        case Some(value) => Async[F].pure(value)
        case None => Async[F].raiseError(new RuntimeException(s"Cannot create Servable from kube Service $service"))
      }
    } yield newServable
  }

  override def remove(name: String): F[Unit] = client.removeService(name) *> client.removeDeployment(name)
}
