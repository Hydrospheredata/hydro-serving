package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.ModelBuild

import scala.concurrent.Future

/**
  *
  */
trait ModelBuildRepository extends BaseRepository[ModelBuild, Long] {
  def lastByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def listByModelId(id: Long): Future[Seq[ModelBuild]]
}
