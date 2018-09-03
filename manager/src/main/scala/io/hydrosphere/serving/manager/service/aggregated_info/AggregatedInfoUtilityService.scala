package io.hydrosphere.serving.manager.service.aggregated_info

import io.hydrosphere.serving.manager.model.db.Model
import io.hydrosphere.serving.model.api.HFResult

import scala.concurrent.Future


trait AggregatedInfoUtilityService {
  def getModelBuilds(modelId: Long): Future[Seq[AggregatedModelBuild]]

  def allModelVersions: Future[Seq[AggregatedModelVersion]]

  def allModelsAggregatedInfo(): Future[Seq[AggregatedModelInfo]]

  def getModelAggregatedInfo(id: Long): HFResult[AggregatedModelInfo]

  def deleteModel(modelId: Long): HFResult[Model]
}
