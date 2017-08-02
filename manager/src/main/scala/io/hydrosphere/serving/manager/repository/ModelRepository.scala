package io.hydrosphere.serving.manager.repository

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.Model

import scala.concurrent.Future

/**
  *
  */
trait ModelRepository extends BaseRepository[Model, Long] {
  def get(name: String): Future[Option[Model]]

  def update(value: Model): Future[Int]

  def updateLastUpdatedTime(source: String, timestamp: LocalDateTime): Future[Int]

  def fetchBySource(source:String):Future[Seq[Model]]
}
