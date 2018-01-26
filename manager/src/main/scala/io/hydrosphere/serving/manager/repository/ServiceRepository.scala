package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.{Service, ServiceKeyDescription}

import scala.concurrent.Future

/**
  *
  */
trait ServiceRepository extends BaseRepository[Service, Long] {
  def fetchServices(services: Set[ServiceKeyDescription]): Future[Seq[Service]]

  def fetchByIds(seq: Seq[Long]): Future[Seq[Service]]

  def updateCloudDriveId(serviceId: Long, cloudDriveId: Option[String]): Future[Int]

  def getByServiceName(serviceName: String): Future[Option[Service]]

  def getByModelIds(modelIds: Seq[Long]): Future[Seq[Service]]

  def getLastServiceByModelName(modelName: String): Future[Option[Service]]

  def getLastServiceByModelNameAndVersion(modelName: String, modelVersion: Long): Future[Option[Service]]

  def getByModelVersionIds(modelIds: Seq[Long]): Future[Seq[Service]]

  def getByRuntimeIds(runtimeIds: Set[Long]): Future[Seq[Service]]
}
