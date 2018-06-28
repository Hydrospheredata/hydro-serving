package io.hydrosphere.serving.manager.service.model_build

import java.util.UUID

import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.service.ServiceTask

import scala.concurrent.Future

trait ModelBuildManagmentService {
  def listForModel(modelId: Long): HFResult[Seq[ModelBuild]]

  def lastForModels(ids: Seq[Long]): Future[Seq[ModelBuild]]

  def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]]

  def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def buildModel(buildModelRequest: BuildModelRequest): HFResult[ServiceTask[BuildModelWithScript, ModelVersion]]

  def uploadAndBuild(modelUpload: ModelUpload): HFResult[ServiceTask[BuildModelWithScript, ModelVersion]]

  def delete(buildId: Long): HFResult[ModelBuild]

  def getBuildStatus(id: UUID): HResult[ServiceTask[BuildModelWithScript, ModelVersion]]
}
