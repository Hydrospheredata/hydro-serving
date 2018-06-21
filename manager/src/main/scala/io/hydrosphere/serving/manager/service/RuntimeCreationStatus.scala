package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime
import java.util.UUID

import io.hydrosphere.serving.manager.service.ServiceTaskStatus.ServiceTaskStatus

trait ServiceRequest extends Product with Serializable

object ServiceTaskStatus extends Enumeration {
  type ServiceTaskStatus = Value
  val Running, Failed, Finished = Value
}

sealed trait ServiceTask[Request <: ServiceRequest, Result] {
  def id: UUID
  def startedAt: LocalDateTime
  def request: Request
  def status: ServiceTaskStatus
}

case class ServiceTaskRunning[Request <: ServiceRequest, Result](
  id: UUID,
  startedAt: LocalDateTime,
  request: Request,
  final val status: ServiceTaskStatus = ServiceTaskStatus.Running
) extends ServiceTask[Request, Result]

case class ServiceTaskFailed[Request <: ServiceRequest, Result](
  request: Request,
  id: UUID,
  startedAt: LocalDateTime,
  message: String,
  reason: Option[Throwable],
  final val status: ServiceTaskStatus = ServiceTaskStatus.Failed
) extends ServiceTask[Request, Result]

case class ServiceTaskFinished[Request <: ServiceRequest, Result](
  request: Request,
  id: UUID,
  startedAt: LocalDateTime,
  finishedAt: LocalDateTime,
  result: Result,
  final val status: ServiceTaskStatus = ServiceTaskStatus.Finished
) extends ServiceTask[Request, Result]
