package io.hydrosphere.serving.manager.service.model_build

import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{ModelBuild, ModelVersion}

import scala.concurrent.Future

trait ModelBuildManagmentService {
  def lastForModels(ids: Seq[Long]): Future[Seq[ModelBuild]]

  def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]]

  def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def buildModel(modelId: Long, flatContract: Option[ContractDescription] = None, modelVersion: Option[Long] = None): HFResult[ModelVersion]

  def buildModelVersion(modelBuild: ModelBuild, script: String): HFResult[ModelVersion]
}


