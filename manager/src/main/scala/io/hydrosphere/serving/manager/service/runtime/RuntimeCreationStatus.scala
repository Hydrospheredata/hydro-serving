package io.hydrosphere.serving.manager.service.runtime

import java.time.LocalDateTime
import java.util.UUID

sealed trait ServiceTaskStatus
case object ServiceTaskRegistered extends ServiceTaskStatus
case object ServiceTaskInProgress extends ServiceTaskStatus
case object ServiceTaskFinished extends ServiceTaskStatus
case object ServiceTaskFailed extends ServiceTaskStatus

trait ServiceRequest

trait ServiceTaskResult[R <: ServiceRequest] {
  def id: UUID
  def startedAt: LocalDateTime
  def request: R
  def status: ServiceTaskStatus
}

trait ServiceTaskFailed[R <: ServiceRequest] extends ServiceTaskResult[R] {
  def message: String
  def reason: Option[Throwable]
}

sealed trait RuntimeCreateStatus extends ServiceTaskResult[CreateRuntimeRequest]

case class RuntimeCreationInProgress(
  id: UUID,
  startedAt: LocalDateTime,
  request: CreateRuntimeRequest,
  final val status: ServiceTaskStatus = ServiceTaskInProgress
) extends RuntimeCreateStatus

case class RuntimeCreationFailed(
  request: CreateRuntimeRequest,
  id: UUID,
  startedAt: LocalDateTime,
  message: String,
  reason: Option[Throwable],
  final val status: ServiceTaskStatus = ServiceTaskFailed
) extends RuntimeCreateStatus

case class RuntimeCreated(
  request: CreateRuntimeRequest,
  id: UUID,
  startedAt: LocalDateTime,
  finishedAt: LocalDateTime,
  result: String,
  final val status: ServiceTaskStatus = ServiceTaskFinished
) extends RuntimeCreateStatus
