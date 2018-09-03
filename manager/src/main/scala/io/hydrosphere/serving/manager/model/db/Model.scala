package io.hydrosphere.serving.manager.model.db

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.grpc.entities.{Model => GModel}
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.DataProfileFields
import io.hydrosphere.serving.model.api.ModelType


case class Model(
  id: Long,
  name: String,
  modelType: ModelType,
  description: Option[String],
  modelContract: ModelContract,
  created: LocalDateTime,
  updated: LocalDateTime,
  dataProfileTypes: Option[DataProfileFields] = None,
) {
  def toGrpc = GModel(
    id = id,
    name = name
  )
}
