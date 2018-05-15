package io.hydrosphere.serving.manager.service.source

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.api.ModelType

case class StorageUploadResult(
  name: String,
  modelType: ModelType,
  description: Option[String],
  modelContract: ModelContract
)
