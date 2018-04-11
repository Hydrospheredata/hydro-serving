package io.hydrosphere.serving.manager.repository

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.ModelBuildStatus.ModelBuildStatus
import io.hydrosphere.serving.manager.model.db.{ModelBuild, ModelVersion}

import scala.concurrent.Future

/**
  *
  */
trait ModelBuildRepository extends BaseRepository[ModelBuild, Long] {
  def lastForModels(id: Seq[Long]): Future[Seq[ModelBuild]]

  def lastByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def listByModelId(id: Long): Future[Seq[ModelBuild]]

  def finishBuild(id: Long, status: ModelBuildStatus, statusText: String, finished: LocalDateTime,
    modelRuntime: Option[ModelVersion]): Future[Int]
}
