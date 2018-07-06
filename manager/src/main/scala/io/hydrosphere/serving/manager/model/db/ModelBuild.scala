package io.hydrosphere.serving.manager.model.db

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.db.ModelBuild.BuildStatus
import io.hydrosphere.serving.manager.util.task.ServiceTask
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus.ServiceTaskStatus

case class BuildRequest (
  model: Model,
  version: Long,
  script: String
)

case class ModelBuild(
  id: Long,
  model: Model,
  version: Long,
  started: LocalDateTime,
  finished: Option[LocalDateTime] = None,
  status: ServiceTaskStatus,
  statusText: Option[String],
  logsUrl: Option[String],
  modelVersion: Option[ModelVersion],
  script: String
) {
  def toBuildTask: BuildStatus = {
    ServiceTask[BuildRequest, ModelVersion](
      id = this.id,
      startedAt = this.started,
      request = BuildRequest(
        model = this.model,
        version = this.version,
        script = this.script
      ),
      status = this.status,
      logsUrl = this.logsUrl,
      result = this.modelVersion,
      finishedAt = this.finished,
      message = this.statusText
    )
  }
}

object ModelBuild {
  type BuildStatus = ServiceTask[BuildRequest, ModelVersion]

  def fromBuildTask(bt: BuildStatus): ModelBuild = {
    ModelBuild(
      id = bt.id,
      model = bt.request.model,
      version = bt.request.version,
      started = bt.startedAt,
      finished = bt.finishedAt,
      status = bt.status,
      statusText = bt.message,
      logsUrl = bt.logsUrl,
      modelVersion = bt.result,
      script = bt.request.script
    )
  }
}
