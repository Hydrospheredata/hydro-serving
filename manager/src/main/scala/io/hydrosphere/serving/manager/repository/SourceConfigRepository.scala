package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.db.ModelSourceConfig

import scala.concurrent.Future

trait SourceConfigRepository extends BaseRepository[ModelSourceConfig, Long] {
  def getByName(name: String): Future[Option[ModelSourceConfig]]
}
