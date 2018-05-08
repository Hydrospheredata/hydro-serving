package io.hydrosphere.serving.manager.service.aggregated_info

import io.hydrosphere.serving.manager.model._

import scala.concurrent.Future


trait AggregatedInfoUtilityService {
  def getModelBuilds(modelId: Long): Future[Seq[AggregatedModelBuild]]

  def allModelVersions: Future[Seq[AggregatedModelVersion]]

  def allModelsAggregatedInfo(): Future[Seq[AggregatedModelInfo]]

  def getModelAggregatedInfo(id: Long): HFResult[AggregatedModelInfo]
}
