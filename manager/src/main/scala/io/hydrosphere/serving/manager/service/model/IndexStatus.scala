package io.hydrosphere.serving.manager.service.model

import io.hydrosphere.serving.manager.model.db.Model

sealed trait IndexStatus extends Product {
  def model: Model
}

case class IndexError(model: Model, error: Throwable) extends IndexStatus
case class ModelDeleted(model: Model) extends IndexStatus
case class ModelUpdated(model: Model) extends IndexStatus
