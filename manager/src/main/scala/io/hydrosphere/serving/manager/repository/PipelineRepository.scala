package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.model.Pipeline

import scala.concurrent.Future

/**
  *
  */
trait PipelineRepository extends BaseRepository[Pipeline, Long] {
  def fetchByIds(seq: Seq[Long]): Future[Seq[Pipeline]]
}
