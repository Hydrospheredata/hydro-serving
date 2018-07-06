package io.hydrosphere.serving.manager.model.db

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.util.task.ServiceTask
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus.ServiceTaskStatus

case class CreateRuntimeRequest(
  name: String,
  version: String,
  modelTypes: List[String] = List.empty,
  tags: List[String] = List.empty,
  configParams: Map[String, String] = Map.empty
) {
  def fullImage = name + ":" + version
}

case class PullRuntime(
  id: Long,
  name: String,
  version: String,
  suitableModelTypes: List[String] = List.empty,
  tags: List[String],
  configParams: Map[String, String] = Map.empty,
  startedAt: LocalDateTime,
  finishedAt: Option[LocalDateTime] = None,
  status: ServiceTaskStatus,
  statusText: Option[String] = None,
  logsUrl: Option[String] = None,
  runtime: Option[Runtime] = None
) {
  def fullImage: String = name + ":" + version

  def toServiceTask: ServiceTask[CreateRuntimeRequest, Runtime] = {
    ServiceTask[CreateRuntimeRequest, Runtime](
      id = this.id,
      startedAt = this.startedAt,
      finishedAt = this.finishedAt,
      status = this.status,
      logsUrl = this.logsUrl,
      message = this.statusText,
      request = CreateRuntimeRequest(
        name = this.name,
        version = this.version,
        modelTypes = this.suitableModelTypes,
        tags = this.tags,
        configParams = this.configParams
      ),
      result = this.runtime
    )
  }
}

object PullRuntime {
  def fromServiceTask(task: ServiceTask[CreateRuntimeRequest, Runtime]): PullRuntime = {
    PullRuntime(
      id = task.id,
      name = task.request.name,
      version = task.request.version,
      suitableModelTypes = task.request.modelTypes,
      tags = task.request.tags,
      configParams = task.request.configParams,
      startedAt = task.startedAt,
      finishedAt = task.finishedAt,
      status = task.status,
      statusText = task.message,
      logsUrl = task.logsUrl,
      runtime = task.result
    )
  }
}