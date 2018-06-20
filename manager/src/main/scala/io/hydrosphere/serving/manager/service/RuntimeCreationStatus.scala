package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime
import java.util.UUID

import io.hydrosphere.serving.manager.model.HFResult

trait ServiceRequest extends Product with Serializable

sealed trait ServiceTask[Request <: ServiceRequest, Result] {
  def id: UUID
  def startedAt: LocalDateTime
  def request: Request
  def status: String
}

case class ServiceTaskRunning[Request <: ServiceRequest, Result](
  id: UUID,
  startedAt: LocalDateTime,
  request: Request,
  final val status: String = "Running"
) extends ServiceTask[Request, Result]

case class ServiceTaskFailed[Request <: ServiceRequest, Result](
  request: Request,
  id: UUID,
  startedAt: LocalDateTime,
  message: String,
  reason: Option[Throwable],
  final val status: String = "Failed"
) extends ServiceTask[Request, Result]

case class ServiceTaskFinished[Request <: ServiceRequest, Result](
  request: Request,
  id: UUID,
  startedAt: LocalDateTime,
  finishedAt: LocalDateTime,
  result: Result,
  final val status: String = "Finished"
) extends ServiceTask[Request, Result]
