package io.hydrosphere.serving.manager.service.model_build

import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{BuildRequest, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.util.task.ExecFuture

import scala.concurrent.Future

trait ModelBuildManagmentService {
  def listForModel(modelId: Long): HFResult[Seq[ModelBuild]]

  def lastForModels(ids: Seq[Long]): Future[Seq[ModelBuild]]

  def modelBuildsByModelId(id: Long): Future[Seq[ModelBuild]]

  def lastModelBuildsByModelId(id: Long, maximum: Int): Future[Seq[ModelBuild]]

  def buildModel(buildModelRequest: BuildModelRequest): HFResult[ExecFuture[BuildRequest, ModelVersion]]

  def uploadAndBuild(modelUpload: ModelUpload): HFResult[ExecFuture[BuildRequest, ModelVersion]]

  def delete(buildId: Long): HFResult[ModelBuild]

  def get(id: Long): HFResult[ModelBuild]
}