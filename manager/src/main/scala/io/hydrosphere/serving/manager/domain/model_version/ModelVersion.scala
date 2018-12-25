package io.hydrosphere.serving.manager.domain.model_version

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.ModelVersionStatus.ModelVersionStatus
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.monitoring.data_profile_types.DataProfileType


case class ModelVersion(
  id: Long,
  image: DockerImage,
  created: LocalDateTime,
  finished: Option[LocalDateTime],
  modelVersion: Long,
  modelType: ModelType,
  modelContract: ModelContract,
  runtime: DockerImage,
  model: Model,
  hostSelector: Option[HostSelector],
  status: ModelVersionStatus,
  profileTypes: Map[String, DataProfileType]
)
