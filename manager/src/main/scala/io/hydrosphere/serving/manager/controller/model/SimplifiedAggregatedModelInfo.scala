package io.hydrosphere.serving.manager.controller.model

import io.hydrosphere.serving.manager.service.AggregatedModelInfo

case class SimplifiedAggregatedModelInfo(
  model: SimplifiedModel,
  lastModelBuild: Option[SimplifiedModelBuild],
  lastModelVersion: Option[SimplifiedModelVersion]
)

object SimplifiedAggregatedModelInfo {
  import io.hydrosphere.serving.manager.model.ManagerJsonSupport._
  implicit val sBuildFormat = jsonFormat3(SimplifiedAggregatedModelInfo.apply)

  def convertFrom(aggregatedModelInfo: AggregatedModelInfo): SimplifiedAggregatedModelInfo = {
    SimplifiedAggregatedModelInfo(
      SimplifiedModel.convertFrom(aggregatedModelInfo.model),
      aggregatedModelInfo.lastModelBuild.map(SimplifiedModelBuild.convertFrom),
      aggregatedModelInfo.lastModelVersion.map(SimplifiedModelVersion.convertFrom)
    )
  }
}