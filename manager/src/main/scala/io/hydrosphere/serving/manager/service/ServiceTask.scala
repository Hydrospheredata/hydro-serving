package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime
import java.util.UUID

import io.hydrosphere.serving.manager.service.ServiceTask.ServiceTaskStatus
import io.hydrosphere.serving.manager.service.ServiceTask.ServiceTaskStatus.ServiceTaskStatus

case class ServiceTask[Request, Result](
  id: UUID,
  startedAt: LocalDateTime,
  request: Request,
  status: ServiceTaskStatus,
  logs: List[String] = List.empty,
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

  def log(logStr: String) = {
    this.copy(logs = this.logs :+ logStr)
  }
}

object ServiceTask {

  object ServiceTaskStatus extends Enumeration {
    type ServiceTaskStatus = Value
    val Pending, Running, Failed, Finished = Value
  }

  def create[Req, Res](id: UUID, startedAt: LocalDateTime, request: Req) = {
    ServiceTask[Req, Res](id, startedAt, request, ServiceTaskStatus.Pending)
  }
}