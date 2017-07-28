package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.ModelService

import scala.concurrent.Future

/**
  *
  */
trait ModelServiceRepository extends BaseRepository[ModelService, Long] {
  def updateCloudDriveId(serviceId: Long, cloudDriveId: Option[String]): Future[Int]

  def getByServiceName(serviceName:String): Future[Option[ModelService]]
}
