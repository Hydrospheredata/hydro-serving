package io.hydrosphere.serving.manager.model.db

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.DataProfileFields
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.manager.grpc.entities.{ModelVersion => GModelVersion}

case class ModelVersion(
  id: Long,
  imageName: String,
  imageTag: String,
  imageSHA256: String,
  created: LocalDateTime,
  modelName: String,
  modelVersion: Long,
  modelType: ModelType,
  model: Option[Model],
  modelContract: ModelContract,
  dataProfileTypes: Option[DataProfileFields] = None,
) {
  def toImageDef: String = imageName + ":" + imageTag
  def fullName: String = modelName + ":" + modelVersion
  def toGrpc = GModelVersion(
    id = id,
    imageName = imageName,
    imageTag = imageTag,
    model = model.map(_.toGrpc),
    contract = Some(modelContract),
    dataTypes = dataProfileTypes.getOrElse(Map.empty)
  )
}
