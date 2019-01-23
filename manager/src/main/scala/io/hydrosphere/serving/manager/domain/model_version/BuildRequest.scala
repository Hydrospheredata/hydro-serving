package io.hydrosphere.serving.manager.domain.model_version

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.model.api.ModelType

case class BuildRequest(
  baseImage: DockerImage,
  modelName: String,
  modelVersion: Long,
  targetImage: DockerImage,
  contract: ModelContract,
)

object BuildRequest {
  def fromVersion(version: ModelVersion) = {
    BuildRequest(
      modelName = version.model.name,
      modelVersion = version.modelVersion,
      targetImage = version.image,
      baseImage = version.runtime,
      contract = version.modelContract
    )
  }
}