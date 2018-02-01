package io.hydrosphere.serving.manager.controller.model

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.ModelBuild
import io.hydrosphere.serving.manager.model.ModelBuildStatus.ModelBuildStatus

case class SimplifiedModelBuild(
  id: Long,
  model: SimplifiedModel,
  version: Long,
  started: LocalDateTime,
  finished: Option[LocalDateTime] = None,
  status: ModelBuildStatus,
  statusText: Option[String],
  logsUrl: Option[String],
  modelVersion: Option[SimplifiedModelVersion]
)

object SimplifiedModelBuild {
  def convertFrom(x: ModelBuild): SimplifiedModelBuild = {
    SimplifiedModelBuild(
      id = x.id,
      model = SimplifiedModel.convertFrom(x.model),
      version = x.version,
      started = x.started,
      finished = x.finished,
      status = x.status,
      statusText = x.statusText,
      logsUrl = x.logsUrl,
      modelVersion = x.modelVersion.map(SimplifiedModelVersion.convertFrom)
    )
  }

  import io.hydrosphere.serving.manager.model.ManagerJsonSupport._
  implicit val sBuildFormat = jsonFormat9(SimplifiedModelBuild.apply)
}