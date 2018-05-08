package io.hydrosphere.serving.manager.service.aggregated_info

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.ModelBuildStatus.ModelBuildStatus
import io.hydrosphere.serving.manager.model.db.{Application, Model, ModelBuild}

case class AggregatedModelBuild(
  id: Long,
  model: Model,
  version: Long,
  started: LocalDateTime,
  finished: Option[LocalDateTime] = None,
  status: ModelBuildStatus,
  statusText: Option[String],
  logsUrl: Option[String],
  modelVersion: Option[AggregatedModelVersion]
)

object AggregatedModelBuild {
  def fromModelBuild(modelBuild: ModelBuild, apps: Seq[Application]): AggregatedModelBuild = {
    AggregatedModelBuild(
      id = modelBuild.id,
      model = modelBuild.model,
      version = modelBuild.version,
      started = modelBuild.started,
      finished = modelBuild.finished,
      status = modelBuild.status,
      statusText = modelBuild.statusText,
      logsUrl = modelBuild.logsUrl,
      modelVersion = modelBuild.modelVersion.map(AggregatedModelVersion.fromModelVersion(_, apps))
    )
  }
}