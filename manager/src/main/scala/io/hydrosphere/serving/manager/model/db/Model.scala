package io.hydrosphere.serving.manager.model.db

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.DataProfileFields
import io.hydrosphere.serving.manager.model.api.ModelType

case class Model(
  id: Long,
  name: String,
  modelType: ModelType,
  description: Option[String],
  modelContract: ModelContract,
  created: LocalDateTime,
  updated: LocalDateTime,
  dataProfileTypes: Option[DataProfileFields] = None,
)
