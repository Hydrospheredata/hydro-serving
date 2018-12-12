package io.hydrosphere.serving.manager.domain.model_version

import com.spotify.docker.client.ProgressHandler
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.model.api.{HFResult, ModelType}

trait ModelBuildAlgebra {
  def build(
    modelName: String,
    modelVersion: Long,
    modelType: ModelType,
    contract: ModelContract,
    image: DockerImage,
    script: String,
    progressHandler: ProgressHandler
  ): HFResult[String] // TODO change to F
}
