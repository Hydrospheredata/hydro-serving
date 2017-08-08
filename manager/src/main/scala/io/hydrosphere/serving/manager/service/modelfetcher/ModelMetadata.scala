package io.hydrosphere.serving.manager.service.modelfetcher

import io.hydrosphere.serving.manager.model.RuntimeType

case class ModelMetadata(
  name: String,
  runtimeType: Option[RuntimeType],
  outputFields: List[String],
  inputFields: List[String]
)
