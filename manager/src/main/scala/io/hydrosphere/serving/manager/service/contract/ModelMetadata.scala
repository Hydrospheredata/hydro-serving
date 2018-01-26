package io.hydrosphere.serving.manager.service.contract

import io.hydrosphere.serving.contract.model_contract.ModelContract

case class ModelMetadata(
  modelName: String,
  modelType: ModelType,
  contract: ModelContract
)