package io.hydrosphere.serving.manager.domain.application

import io.hydrosphere.serving.manager.domain.model_version.ModelVersion

case class ModelVariant(
  modelVersion: ModelVersion,
  weight: Int,
)
