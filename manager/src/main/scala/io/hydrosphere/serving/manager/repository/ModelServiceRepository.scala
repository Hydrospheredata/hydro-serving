package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.model.ModelService

import scala.concurrent.Future

/**
  *
  */
trait ModelServiceRepository extends BaseRepository[ModelService, Long] {
  def fetchByIds(seq: Seq[Long]): Future[Seq[ModelService]]

  def updateCloudDriveId(serviceId: Long, cloudDriveId: Option[String]): Future[Int]

  def getByServiceName(serviceName:String): Future[Option[ModelService]]

  def getByModelIds(modelIds:Seq[Long]): Future[Seq[ModelService]]

  def getLastModelServiceByModelName(modelName:String): Future[Option[ModelService]]

  def getLastModelServiceByModelNameAndVersion(modelName:String, modelVersion:String): Future[Option[ModelService]]

  def getByModelRuntimeIds(modelIds:Seq[Long]): Future[Seq[ModelService]]
}
