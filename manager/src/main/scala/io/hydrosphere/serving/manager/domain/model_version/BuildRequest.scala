package io.hydrosphere.serving.manager.domain.model_version

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.model.api.ModelType

case class BuildRequest(
  modelName: String,
  modelVersion: Long,
  modelType: ModelType,
  contract: ModelContract,
  image: DockerImage,
  script: String
)

object BuildRequest {
  def fromVersion(version: ModelVersion, script: String) = {
    BuildRequest(
      modelName = version.model.name,
      modelVersion = version.modelVersion,
      modelType = version.modelType,
      contract = version.modelContract,
      image = version.image,
      script = script
    )
  }
}