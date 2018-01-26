package io.hydrosphere.serving.manager.model

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.util.CommonJsonSupport._
import io.hydrosphere.serving.manager.model.ModelBuildStatus.ModelBuildStatus

case class ModelBuild(
  id: Long,
  model: Model,
  version: Long,
  started: LocalDateTime,
  finished: Option[LocalDateTime] = None,
  status: ModelBuildStatus,
  statusText: Option[String],
  logsUrl: Option[String],
  modelVersion: Option[ModelVersion]
)

object ModelBuild {
  implicit val modelBuildFormat = jsonFormat9(ModelBuild.apply)
}