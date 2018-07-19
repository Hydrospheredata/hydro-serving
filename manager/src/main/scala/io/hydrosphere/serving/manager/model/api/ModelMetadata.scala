package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.contract.model_contract.ModelContract

case class ModelMetadata(
  modelName: String,
  modelType: ModelType,
  modelContract: ModelContract,
  description: Option[String] = None
)