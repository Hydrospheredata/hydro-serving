package io.hydrosphere.serving.manager.util.task

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus.ServiceTaskStatus

case class ServiceTask[Request, Result](
  id: Long,
  startedAt: LocalDateTime,
  request: Request,
  status: ServiceTaskStatus,
  logsUrl: Option[String] = None,
  result: Option[Result] = None,
  finishedAt: Option[LocalDateTime] = None,
  message: Option[String] = None
) {
  def run(): ServiceTask[Request, Result] = {
    this.copy(status = ServiceTaskStatus.Running)
  }

  def finish(result: Result, time: LocalDateTime) = {
    this.copy(status = ServiceTaskStatus.Finished, result = Some(result), finishedAt = Some(time))
  }

  def fail(message: String, time: LocalDateTime) = {
    this.copy(status = ServiceTaskStatus.Failed, message = Some(message), finishedAt = Some(time))
  }
}

object ServiceTask {

  object ServiceTaskStatus extends Enumeration {
    type ServiceTaskStatus = Value
    val Pending, Running, Failed, Finished = Value
  }

  def create[Req, Res](id: Long, startedAt: LocalDateTime, request: Req) = {
    ServiceTask[Req, Res](id, startedAt, request, ServiceTaskStatus.Pending)
  }
}