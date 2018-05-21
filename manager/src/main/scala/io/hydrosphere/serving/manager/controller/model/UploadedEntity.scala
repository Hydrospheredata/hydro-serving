package io.hydrosphere.serving.manager.controller.model

import java.nio.file.Path

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.{HResult, Result}

import scala.reflect.ClassTag

sealed trait UploadedEntity extends Product with Serializable

case class UnknownPart(name: String) extends UploadedEntity

case class UploadTarball(path: Path) extends UploadedEntity

case class UploadModelName(modelName: String) extends UploadedEntity

case class UploadModelType(modelType: String) extends UploadedEntity

case class UploadDescription(description: String) extends UploadedEntity

case class UploadContract(modelContract: ModelContract) extends UploadedEntity

case class UploadRemotePayload(link: String) extends UploadedEntity

object Entities {
  val modelType = "model_type"
  val modelContract = "model_contract"
  val modelDescription = "model_description"
  val modelName = "model_name"
  val payload = "payload"
  val remotePayload = "remote_payload"
}