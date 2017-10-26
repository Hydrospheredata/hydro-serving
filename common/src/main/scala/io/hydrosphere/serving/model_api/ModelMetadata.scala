package io.hydrosphere.serving.model_api

import io.hydrosphere.serving.model.RuntimeType

case class ModelMetadata(
  name: String,
  runtimeType: Option[RuntimeType],
  outputFields: ModelApi,
  inputFields: ModelApi
)