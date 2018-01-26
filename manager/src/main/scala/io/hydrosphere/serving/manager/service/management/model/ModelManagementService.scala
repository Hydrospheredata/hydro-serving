package io.hydrosphere.serving.manager.service.management.model

import io.hydrosphere.serving.manager.model.{Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.service.contract.description.ContractDescription
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import spray.json.JsObject

import scala.concurrent.Future

trait ModelManagementService {

  def submitBinaryContract(modelId: Long, bytes: Array[Byte]): Future[Option[Model]]

  def submitFlatContract(modelId: Long, contractDescription: ContractDescription): Future[Option[Model]]

  def submitContract(modelId: Long, prototext: String): Future[Option[Model]]

  def buildModel(modelId: Long, modelVersion: Option[Long]): Future[ModelVersion]

  def allModels(): Future[Seq[Model]]

  def updateModel(entity: CreateOrUpdateModelRequest): Future[Model]

  def updateModel(modelName: String, modelSource: ModelSource): Future[Option[Model]]

  def createModel(entity: CreateOrUpdateModelRequest): Future[Model]

  def updatedInModelSource(entity: Model): Future[Unit]

  def addModelVersion(entity: CreateModelVersionRequest): Future[ModelVersion]

  def allModelVersion(): Future[Seq[ModelVersion]]

  def generateModelPayload(modelId: Long, signature: String): Future[Seq[JsObject]]

  def generateInputsForVersion(versionId: Long, signature: String): Future[Seq[JsObject]]

  def lastModelVersionByModelId(id: Long, maximum: Int): Future[Seq[ModelVersion]]

  def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]]

  def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def modelsByType(types: Set[String]): Future[Seq[Model]]
}
