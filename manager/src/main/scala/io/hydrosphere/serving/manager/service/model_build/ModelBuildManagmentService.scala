package io.hydrosphere.serving.manager.service.model_build

import java.nio.file.Path

import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.manager.controller.model.{ModelDeploy, ModelUpload}
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{Application, ModelBuild}

import scala.concurrent.Future

trait ModelBuildManagmentService {
  def lastForModels(ids: Seq[Long]): Future[Seq[ModelBuild]]

  def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]]

  def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def buildAndOverrideContract(modelId: Long, flatContract: Option[ContractDescription] = None, modelVersion: Option[Long] = None): HFResult[ModelBuild]

  def uploadAndBuild(file: Path, modelUpload: ModelUpload): HFResult[ModelBuild]

  def uploadAndDeploy(file: Path, modelUpload: ModelDeploy): HFResult[Application]
}


