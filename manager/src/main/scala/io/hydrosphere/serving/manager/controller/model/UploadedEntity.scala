package io.hydrosphere.serving.manager.controller.model

import java.nio.file.Path

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.{HResult, Result}

import scala.reflect.ClassTag

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
    contract: Option[ModelContract],
    description: Option[String] = None,
    source: Option[String] = None,
    tarballPath: Path
  ) extends UploadedEntity

  object ModelUpload {
    def extractAs[T](map: Map[String, UploadedEntity], fieldName: String)(implicit ct: ClassTag[T]): HResult[T] = {
      map.get(fieldName) match {
        case Some(field) =>
          field match {
            case ct(x) => Right(x)
            case x => Result.clientError(s"'$fieldName' has incompatible type. Expected: ${ct.runtimeClass}, got ${x.getClass}")
          }
        case None => Result.clientError(s"'$fieldName' part is missing")
      }
    }

    def fromMap(map: Map[String, UploadedEntity]): HResult[ModelUpload] = {
      for {
        modelType <- extractAs[UploadType](map, "model_type").right
        modelName <- extractAs[ModelName](map, "model_name").right
        tarball <- extractAs[Tarball](map, "payload").right
      } yield ModelUpload(
        name = modelName.asInstanceOf[ModelName].modelName,
        modelType = modelType.asInstanceOf[UploadType].modelType,
        tarballPath = tarball.asInstanceOf[Tarball].path,
        contract = map.get("model_contract").map(_.asInstanceOf[Contract].modelContract),
        source = map.get("target_source").map(_.asInstanceOf[TargetSource].source),
        description = map.get("model_description").map(_.asInstanceOf[Description].description)
      )
    }
  }

}