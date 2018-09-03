package io.hydrosphere.serving.manager.service.model

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.db.Model
import io.hydrosphere.serving.model.api.ModelType

case class CreateModelRequest(
  name: String,
  modelType: ModelType,
  description: Option[String],
  modelContract: ModelContract
)

case class UpdateModelRequest(
  id: Long,
  name: String,
  modelType: ModelType,
  description: Option[String],
  modelContract: ModelContract
) {
  def fillModel(model: Model) = {
    model.copy(
      name = name,
      modelType = modelType,
      description = description,
      modelContract = modelContract
    )
  }
}