package io.hydrosphere.serving.manager.domain

sealed trait DomainError extends Throwable with Product with Serializable {
  def message: String
}

object DomainError {

  final case class NotFound(message: String) extends DomainError

  final case class InvalidRequest(message: String) extends DomainError

  final case class InternalError(message: String) extends DomainError

  def notFound(message: String): DomainError = NotFound(message)

  def invalidRequest(message: String): DomainError = InvalidRequest(message)

  def internalError(message: String): DomainError = InternalError(message)
}