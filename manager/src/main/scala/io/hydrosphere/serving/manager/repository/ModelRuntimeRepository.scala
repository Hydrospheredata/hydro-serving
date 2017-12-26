package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.model.ModelRuntime

import scala.concurrent.Future

/**
  *
  */
trait ModelRuntimeRepository extends BaseRepository[ModelRuntime, Long] {
  def lastModelRuntimeByModel(modelId: Long, max: Int): Future[Seq[ModelRuntime]]

  def modelRuntimeByModelAndVersion(modelId: Long, version: Long): Future[Option[ModelRuntime]]

  def lastModelRuntimeForModels(modelIds: Seq[Long]): Future[Seq[ModelRuntime]]

  def fetchByTags(tags:Seq[String]): Future[Seq[ModelRuntime]]
}
