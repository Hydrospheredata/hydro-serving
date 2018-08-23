package io.hydrosphere.serving.manager.service.model_version

import io.hydrosphere.serving.manager.model.db.ModelVersion
import io.hydrosphere.serving.model.api.HFResult
import io.hydrosphere.serving.model.api.description.ContractDescription
import spray.json.JsObject

import scala.concurrent.Future

trait ModelVersionManagementService {
  def modelVersionsByModelVersionIds(modelIds: Set[Long]): Future[Seq[ModelVersion]]

  def lastModelVersionForModels(ids: Seq[Long]): Future[Seq[ModelVersion]]

  def create(version: ModelVersion): HFResult[ModelVersion]

  def get(key: Long): HFResult[ModelVersion]

  def list: Future[Seq[ModelVersion]]

  def listForModel(modelId: Long): HFResult[Seq[ModelVersion]]

  def versionContractDescription(versionId: Long): HFResult[ContractDescription]

  def addModelVersion(entity: CreateModelVersionRequest): HFResult[ModelVersion]

  def generateInputsForVersion(versionId: Long, signature: String): HFResult[JsObject]

  def lastModelVersionByModelId(id: Long, maximum: Int): Future[Seq[ModelVersion]]

  def fetchLastModelVersion(modelId: Long, modelVersion: Option[Long]): HFResult[Long]

  def delete(versionId: Long): HFResult[ModelVersion]
}


