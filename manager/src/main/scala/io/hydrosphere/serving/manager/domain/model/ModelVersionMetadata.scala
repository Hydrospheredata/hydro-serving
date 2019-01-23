package io.hydrosphere.serving.manager.domain.model

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.data_profile_types.DataProfileType
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.model.api.ModelType

case class ModelVersionMetadata(
  modelName: String,
  contract: ModelContract,
  profileTypes: Map[String, DataProfileType],
  runtime: DockerImage,
  hostSelector: Option[HostSelector]
)