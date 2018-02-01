package io.hydrosphere.serving.manager.controller.model

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.ModelVersion
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.api.description.ContractDescription
import io.hydrosphere.serving.manager.model.api.ops.Implicits._

case class SimplifiedModelVersion(
  id: Long,
  imageName: String,
  imageTag: String,
  imageSHA256: String,
  created: LocalDateTime,
  modelName: String,
  modelVersion: Long,
  modelType: ModelType,
  source: Option[String],
  model: Option[SimplifiedModel],
  modelContract: ContractDescription
)

object SimplifiedModelVersion {
  def convertFrom(x: ModelVersion): SimplifiedModelVersion = {
    SimplifiedModelVersion(
      id = x.id,
      imageName = x.imageName,
      imageTag = x.imageTag,
      imageSHA256 = x.imageSHA256,
      created = x.created,
      modelName = x.modelName,
      modelVersion = x.modelVersion,
      modelType = x.modelType,
      source = x.source,
      model = x.model.map(SimplifiedModel.convertFrom),
      modelContract = x.modelContract.flatten
    )
  }

  import io.hydrosphere.serving.manager.model.ManagerJsonSupport._

  implicit val sBuildFormat = jsonFormat11(SimplifiedModelVersion.apply)
}