package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.ModelSourceConfigAux

import scala.concurrent.Future

trait SourceConfigRepository extends BaseRepository[ModelSourceConfigAux, Long] {
  def getByName(name: String): Future[Option[ModelSourceConfigAux]]
}
