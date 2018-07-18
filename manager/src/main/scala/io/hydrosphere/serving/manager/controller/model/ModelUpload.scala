package io.hydrosphere.serving.manager.controller.model

import io.hydrosphere.serving.contract.model_contract.ModelContract

case class ModelUpload(
  name: Option[String] = None,
  modelType: Option[String] = None,
  contract: Option[ModelContract] = None,
  description: Option[String] = None,
  namespace: Option[String] = None
)