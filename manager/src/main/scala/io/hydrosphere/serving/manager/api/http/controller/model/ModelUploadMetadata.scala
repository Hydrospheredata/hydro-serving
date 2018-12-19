package io.hydrosphere.serving.manager.api.http.controller.model

import io.hydrosphere.serving.contract.model_contract.ModelContract

case class ModelUploadMetadata(
  name: Option[String] = None,
  modelType: Option[String] = None,
  runtimeName: String,
  runtimeVersion: String,
  hostSelectorName: Option[String] = None,
  contract: Option[ModelContract] = None,
  description: Option[String] = None
)