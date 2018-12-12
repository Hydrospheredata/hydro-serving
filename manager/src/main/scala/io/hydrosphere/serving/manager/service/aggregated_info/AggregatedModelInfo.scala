package io.hydrosphere.serving.manager.service.aggregated_info

import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion

case class AggregatedModelInfo(
  model: Model,
  lastModelVersion: Option[ModelVersion],
  nextVersion: Option[Long]
)
