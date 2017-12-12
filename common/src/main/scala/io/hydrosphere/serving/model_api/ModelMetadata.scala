package io.hydrosphere.serving.model_api

import hydrosphere.contract.model_contract.ModelContract

case class ModelMetadata(
  modelName: String,
  modelType: ModelType,
  contract: ModelContract
)