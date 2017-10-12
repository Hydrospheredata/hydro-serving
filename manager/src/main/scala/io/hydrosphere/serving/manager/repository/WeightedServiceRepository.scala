package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.model.WeightedService

import scala.concurrent.Future

/**
  *
  */
trait WeightedServiceRepository extends BaseRepository[WeightedService, Long] {
  def update(value: WeightedService): Future[Int]

  def byModelServiceIds(servicesIds:Seq[Long]): Future[Seq[WeightedService]]
}
