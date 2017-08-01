package io.hydrosphere.serving.manager.repository

import io.hydrosphere.serving.manager.model.ModelRuntime

import scala.concurrent.Future

/**
  *
  */
trait ModelRuntimeRepository extends BaseRepository[ModelRuntime, Long] {
  def lastModelRuntimeByModel(modelId: Long, max: Int): Future[Seq[ModelRuntime]]

}
