package io.hydrosphere.serving.manager.model

import spray.json.{JsObject, JsString, JsValue, JsonWriter}
import spray.json._

import scala.concurrent.Future

object Result {
  trait HError
  case class ClientError(message: String) extends HError
  case class InternalError[T <: Throwable](exception: T, reason: Option[String] = None) extends HError

  def clientError[T](message: String): HResult[T] = Left(ClientError(message))
  def internalError[T <: Throwable](ex: T): HResult[T] = Left(InternalError(ex, None))
  def internalError[T <: Throwable](ex: T, reason: String): HResult[T] = Left(InternalError(ex, Some(reason)))

  def clientErrorF[T](message: String): HFResult[T] = Future.successful(Left(ClientError(message)))
  def internalErrorF[T <: Throwable](ex: T): HFResult[T] = Future.successful(Left(InternalError(ex, None)))
  def internalErrorF[T <: Throwable](ex: T, reason: String): HFResult[T] = Future.successful(Left(InternalError(ex, Some(reason))))
}