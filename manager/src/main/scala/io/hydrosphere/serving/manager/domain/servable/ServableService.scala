package io.hydrosphere.serving.manager.domain.servable

import cats.Traverse
import cats.effect.Sync
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.hydrosphere.serving.manager.domain.DomainError.NotFound
import io.hydrosphere.serving.manager.domain.clouddriver._
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.manager.infrastructure.envoy.events.ServableDiscoveryEventBus
import org.apache.logging.log4j.scala.Logging

import scala.util.control.NonFatal

trait ServableService[F[_]] {
  def delete(serviceId: Long): F[Servable]

  def deleteServables(services: List[Long]): F[List[Servable]]

  def create(servableName: String, configParams: Option[Map[String, String]], modelVersion: ModelVersion): F[Servable]

  def deployModelVersions(modelVersions: Set[ModelVersion]): F[List[Servable]]
}

object ServableService {
  def apply[F[_] : Sync](
    cloudDriver: CloudDriver[F],
    servableRepository: ServableRepository[F],
    eventPublisher: ServableDiscoveryEventBus[F]
  ): ServableService[F] = new ServableService[F] with Logging {
    private def createAndDeploy(serviceName: String, configParams: Option[Map[String, String]], modelVersion: ModelVersion): F[Servable] = {
      val dService = Servable(
        id = 0,
        serviceName = serviceName,
        cloudDriverId = None,
        modelVersion = modelVersion,
        statusText = "New",
        configParams = configParams.getOrElse(Map.empty)
      )
      val f = for {
        newService <- servableRepository.create(dService)
        cloudService <- cloudDriver.deployService(newService, modelVersion.image, modelVersion.hostSelector)
        _ <- servableRepository.updateCloudDriveId(cloudService.id, Some(cloudService.cloudDriverId))
      } yield newService.copy(cloudDriverId = Some(cloudService.cloudDriverId))

      Sync[F].onError(f) {
        case NonFatal(ex) =>
          logger.error(ex)
          servableRepository.delete(dService.id).map(_ => ())
      }
    }

    def create(serviceName: String, configParams: Option[Map[String, String]], modelVersion: ModelVersion): F[Servable] = {
      //TODO ADD validation for names manager,gateway + length + without space and special symbols
      for {
        asd <- createAndDeploy(serviceName, configParams, modelVersion)
        _ <- eventPublisher.detected(asd)
      } yield asd
    }


    def delete(serviceId: Long): F[Servable] = {
      for {
        servable <- servableRepository.get(serviceId).flatMap {
          case None =>
            Sync[F].raiseError[Servable](NotFound(s"Couldn't find servable id=$serviceId"))
          case Some(x) =>
            Sync[F].pure(x)
        }
        _ <- cloudDriver.removeService(serviceId)
        _ <- eventPublisher.removed(servable)
        _ <- servableRepository.delete(serviceId)
      } yield servable
    }

    def deployModelVersions(modelVersions: Set[ModelVersion]): F[List[Servable]] = {
      Traverse[List].traverse(modelVersions.toList) { mv =>
        create(s"mv${mv.id}", None, mv)
      }
    }

    def deleteServables(services: List[Long]): F[List[Servable]] = {
      Traverse[List].traverse(services) { service =>
        delete(service)
      }
    }
  }
}