package io.hydrosphere.serving.manager.service.model

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.Model

case class CreateOrUpdateModelRequest(
  id: Option[Long],
  name: String,
  modelType: ModelType,
  description: Option[String],
  modelContract: ModelContract
) {
  def toModel: Model = {
    Model(
      id = 0,
      name = this.name,
      modelType = this.modelType,
      description = this.description,
      modelContract = this.modelContract,
      created = LocalDateTime.now(),
      updated = LocalDateTime.now()
    )
  }

  def toModel(model: Model): Model = {
    model.copy(
      name = this.name,
      modelType = this.modelType,
      description = this.description,
      modelContract = this.modelContract
    )
  }
}
