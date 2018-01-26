package io.hydrosphere.serving.manager.model

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.util.CommonJsonSupport._
import io.hydrosphere.serving.manager.service.contract.ModelType

case class Model(
  id: Long,
  name: String,
  source: String,
  modelType: ModelType,
  description: Option[String],
  modelContract: ModelContract,
  created: LocalDateTime,
  updated: LocalDateTime
)

object Model {
  implicit val modelFormat = jsonFormat8(Model.apply)
}