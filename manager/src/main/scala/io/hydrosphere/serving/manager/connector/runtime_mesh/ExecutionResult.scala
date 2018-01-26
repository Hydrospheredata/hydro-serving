package io.hydrosphere.serving.manager.connector.runtime_mesh

import akka.http.scaladsl.model.StatusCode

sealed trait ExecutionResult {
  def statusCode: StatusCode
}

object ExecutionResult {
  case class ExecutionSuccess(
    json: Array[Byte],
    statusCode: StatusCode
  ) extends ExecutionResult

  case class ExecutionFailure(
    error: String,
    statusCode: StatusCode
  ) extends ExecutionResult
}