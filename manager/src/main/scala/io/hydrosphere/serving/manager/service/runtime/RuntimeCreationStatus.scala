package io.hydrosphere.serving.manager.service.runtime

import java.time.LocalDateTime
import java.util.UUID

trait ServiceRequest

trait ServiceTaskResult[R <: ServiceRequest] {
  def id: UUID
  def startedAt: LocalDateTime
  def request: R
  def status: String
}

trait ServiceTaskFailed[R <: ServiceRequest] extends ServiceTaskResult[R] {
  def message: String
  def reason: Option[Throwable]
}

sealed trait RuntimeCreateStatus extends ServiceTaskResult[CreateRuntimeRequest]

case class RuntimeCreationRunning(
  id: UUID,
  startedAt: LocalDateTime,
  request: CreateRuntimeRequest,
  final val status: String = "Running"
) extends RuntimeCreateStatus

case class RuntimeCreationFailed(
  request: CreateRuntimeRequest,
  id: UUID,
  startedAt: LocalDateTime,
  message: String,
  reason: Option[Throwable],
  final val status: String = "Failed"
) extends RuntimeCreateStatus

case class RuntimeCreated(
  request: CreateRuntimeRequest,
  id: UUID,
  startedAt: LocalDateTime,
  finishedAt: LocalDateTime,
  result: String,
  final val status: String = "Finished"
) extends RuntimeCreateStatus
