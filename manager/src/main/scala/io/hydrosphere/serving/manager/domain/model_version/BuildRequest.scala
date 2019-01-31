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
  installCommand: Option[String]
)

object BuildRequest {
  def fromVersion(version: ModelVersion, installCommand: Option[String]) = {
    BuildRequest(
      modelName = version.model.name,
      modelVersion = version.modelVersion,
      targetImage = version.image,
      baseImage = version.runtime,
      contract = version.modelContract,
      installCommand = installCommand
    )
  }
}