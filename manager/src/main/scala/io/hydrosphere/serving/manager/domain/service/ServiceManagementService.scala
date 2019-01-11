package io.hydrosphere.serving.manager.domain.service

import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.manager.domain.clouddriver._
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorService
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionService}
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.InternalManagerEventsPublisher
import io.hydrosphere.serving.model.api.{HFResult, Result}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ServiceManagementService(
  cloudDriver: CloudDriverAlgebra[Future],
  serviceRepository: ServiceRepositoryAlgebra[Future],
  versionManagementService: ModelVersionService,
  environmentManagementService: HostSelectorService,
  internalManagerEventsPublisher: InternalManagerEventsPublisher
)(implicit val ex: ExecutionContext) extends Logging {

  private def syncServices(services: Seq[Service]): Future[Seq[Service]] =
    cloudDriver.services(services.map(s => s.id).toSet).map(cloudServices => {
      val map = cloudServices.map(p => p.id -> p).toMap
      services.map(s => {
        map.get(s.id) match {
          case Some(cs) =>
            s.copy(statusText = cs.statusText)
          case _ =>
            s.copy(statusText = "Unknown")
        }
      })
    })


  def allServices(): Future[Seq[Service]] =
    serviceRepository.all()

  def getAllCloudServices: Future[Seq[CloudService]] = {
    cloudDriver.serviceList()
  }

  def createAndDeploy(
    serviceName: String,
    configParams: Option[Map[String, String]],
    modelVersion: ModelVersion
  ): Future[Service] = {
    val dService = Service(
      id = 0,
      serviceName = serviceName,
      cloudDriverId = None,
      modelVersion = modelVersion,
      statusText = "New",
      configParams = configParams.getOrElse(Map.empty)
    )
    val f = for {
      newService <- serviceRepository.create(dService)
      cloudService <- cloudDriver.deployService(newService, modelVersion.runtime, modelVersion.image, modelVersion.hostSelector)
      _ <- serviceRepository.updateCloudDriveId(cloudService.id, Some(cloudService.cloudDriverId))
    } yield newService.copy(cloudDriverId = Some(cloudService.cloudDriverId))

    f.failed.foreach { _ =>
      serviceRepository.delete(dService.id)
    }
    f
  }

  def addService(serviceName: String, configParams: Option[Map[String, String]], modelVersion: ModelVersion): HFResult[Service] = {
    logger.debug(serviceName)
    //TODO ADD validation for names manager,gateway + length + without space and special symbols
    val f = for {
      asd <- EitherT(createAndDeploy(serviceName, configParams, modelVersion).map(Result.ok))
    } yield {
      internalManagerEventsPublisher.serviceChanged(asd)
      asd
    }
    f.value
  }


  //TODO check service in applications before delete
  def deleteService(serviceId: Long): HFResult[Service] =
    serviceRepository.get(serviceId).flatMap {
      case Some(x) =>
        for {
          _ <- cloudDriver.removeService(serviceId)
          _ <- serviceRepository.delete(serviceId)
        } yield {
          internalManagerEventsPublisher.serviceRemoved(x)
          Result.ok(x)
        }
      case None =>
        Result.clientErrorF(s"Can't find service id=$serviceId")
    }

  def servicesByIds(ids: Seq[Long]): Future[Seq[Service]] =
    serviceRepository.fetchByIds(ids)
      .flatMap(syncServices)


  def fetchServicesUnsync(services: Set[Long]): Future[Seq[Service]] = {
    serviceRepository.fetchServices(services)
  }
}