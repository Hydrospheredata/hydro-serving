package io.hydrosphere.serving.manager.model

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.util.CommonJsonSupport._
import io.hydrosphere.serving.manager.service.contract.ModelType

case class ModelVersion(
  id: Long,
  imageName: String,
  imageTag: String,
  imageSHA256: String,
  created: LocalDateTime,
  modelName: String,
  modelVersion: Long,
  modelType: ModelType,
  source: Option[String],
  model: Option[Model],
  modelContract: ModelContract
) {
  def toImageDef: String = s"$imageName:$imageTag"
}

object ModelVersion {
  implicit val modelVersionFormat = jsonFormat11(ModelVersion.apply)
}
