package io.hydrosphere.serving.manager.domain.model_version

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.ModelVersionStatus.ModelVersionStatus
import io.hydrosphere.serving.manager.data_profile_types.DataProfileType

case class ModelVersion(
  id: Long,
  image: DockerImage,
  created: LocalDateTime,
  finished: Option[LocalDateTime],
  modelVersion: Long,
  modelContract: ModelContract,
  runtime: DockerImage,
  model: Model,
  hostSelector: Option[HostSelector],
  status: ModelVersionStatus,
  profileTypes: Map[String, DataProfileType],
  installCommand: Option[String],
  metadata: Map[String, String]
) {
  def servableName: String = s"${model.name}-$id".replace("_", "-")
}
