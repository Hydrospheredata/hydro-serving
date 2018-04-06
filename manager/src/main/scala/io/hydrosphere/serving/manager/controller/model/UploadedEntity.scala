package io.hydrosphere.serving.manager.controller.model

import java.nio.file.Path

import io.hydrosphere.serving.contract.model_contract.ModelContract

sealed trait UploadedEntity {
  def name: String
}

object UploadedEntity {

  case class UnknownPart(name: String) extends UploadedEntity

  case class Tarball(name: String = "payload", path: Path) extends UploadedEntity

  case class ModelName(name: String = "model_name", modelName: String) extends UploadedEntity

  case class UploadType(name: String = "model_type", modelType: String) extends UploadedEntity

  case class TargetSource(name: String = "target_source", source: String) extends UploadedEntity

  case class Description(name: String = "model_description", description: String) extends UploadedEntity

  case class Contract(name: String = "model_contract", modelContract: ModelContract) extends UploadedEntity

  case class ModelUpload(
    name: String,
    modelType: String,
    contract: ModelContract,
    description: Option[String] = None,
    source: Option[String] = None,
    tarballPath: Path
  ) extends UploadedEntity

  object ModelUpload {
    def fromMap(map: Map[String, UploadedEntity]): Option[ModelUpload] = {
      for {
        modelType <- map.get("model_type")
        modelName <- map.get("model_name")
        contract <- map.get("model_contract")
        tarball <- map.get("payload")
      } yield ModelUpload(
        name = modelName.asInstanceOf[ModelName].modelName,
        modelType = modelType.asInstanceOf[UploadType].modelType,
        tarballPath = tarball.asInstanceOf[Tarball].path,
        contract = contract.asInstanceOf[Contract].modelContract,
        source = map.get("target_source").map(_.asInstanceOf[TargetSource].source),
        description = map.get("model_description").map(_.asInstanceOf[Description].description)
      )
    }
  }

}