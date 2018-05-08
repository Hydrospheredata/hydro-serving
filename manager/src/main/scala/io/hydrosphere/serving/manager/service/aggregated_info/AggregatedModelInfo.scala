package io.hydrosphere.serving.manager.service.aggregated_info

import io.hydrosphere.serving.manager.model.db._

case class AggregatedModelInfo(
  model: Model,
  lastModelBuild: Option[ModelBuild],
  lastModelVersion: Option[ModelVersion],
  nextVersion: Option[Long]
)
