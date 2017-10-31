package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.model.Application

import scala.concurrent.Future

/**
  *
  */
trait ApplicationRepository extends BaseRepository[Application, Long] {
  def update(value: Application): Future[Int]

  def byModelServiceIds(servicesIds:Seq[Long]): Future[Seq[Application]]
}
