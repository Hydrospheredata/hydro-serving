package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.db.ModelBuild

import scala.concurrent.Future

trait ModelBuildRepository extends BaseRepository[ModelBuild, Long] {
  def lastForModels(id: Seq[Long]): Future[Seq[ModelBuild]]

  def lastByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def listByModelId(id: Long): Future[Seq[ModelBuild]]

  def getRunningBuild(modelId: Long, modelVersion: Long): Future[Option[ModelBuild]]
}