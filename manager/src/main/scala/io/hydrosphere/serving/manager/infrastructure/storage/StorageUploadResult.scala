package io.hydrosphere.serving.manager.infrastructure.storage

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.model.api.ModelType

case class StorageUploadResult(
  name: String,
  modelType: ModelType,
  description: Option[String],
  modelContract: ModelContract
)
