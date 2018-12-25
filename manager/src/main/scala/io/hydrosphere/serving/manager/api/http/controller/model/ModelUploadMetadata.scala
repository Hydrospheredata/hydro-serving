package io.hydrosphere.serving.manager.api.http.controller.model

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.monitoring.data_profile_types.DataProfileType

case class ModelUploadMetadata(
  name: Option[String] = None,
  modelType: Option[ModelType] = None,
  runtimeName: String,
  runtimeVersion: String,
  hostSelectorName: Option[String] = None,
  contract: Option[ModelContract] = None,
  description: Option[String] = None,
  profileTypes: Option[Map[String, DataProfileType]] = None
)