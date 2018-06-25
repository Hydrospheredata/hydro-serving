package io.hydrosphere.serving.manager.service.model_build

import io.hydrosphere.serving.manager.model.api.description.ContractDescription

case class BuildModelRequest (
  modelId: Long,
  flatContract: Option[ContractDescription] = None,
  modelVersion: Option[Long] = None
)
