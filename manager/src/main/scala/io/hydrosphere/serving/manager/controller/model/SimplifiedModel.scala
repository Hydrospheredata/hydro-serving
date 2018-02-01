package io.hydrosphere.serving.manager.controller.model

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.Model
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.api.description.ContractDescription

import io.hydrosphere.serving.manager.model.api.ops.Implicits._

case class SimplifiedModel(
  id: Long,
  name: String,
  source: String,
  modelType: ModelType,
  description: Option[String],
  modelContract: ContractDescription,
  created: LocalDateTime,
  updated: LocalDateTime
)

object SimplifiedModel {
  def convertFrom(model: Model): SimplifiedModel = {
    SimplifiedModel(
      id = model.id,
      name = model.name,
      source = model.source,
      modelType = model.modelType,
      description = model.description,
      modelContract = model.modelContract.flatten,
      created = model.created,
      updated = model.updated
    )
  }

  import io.hydrosphere.serving.manager.model.ManagerJsonSupport._

  implicit val sModelFormat = jsonFormat8(SimplifiedModel.apply)
}