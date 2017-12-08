package io.hydrosphere.serving.model_api

import hydroserving.contract.model_contract.ModelContract

case class ModelMetadata(
  modelName: String,
  modelType: ModelType,
  contract: ModelContract
)