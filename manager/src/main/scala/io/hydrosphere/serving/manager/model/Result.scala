package io.hydrosphere.serving.manager.model

import scala.concurrent.Future

object Result {

  object Implicits {
    implicit class OptResult[T](opt: Option[T]) {
      def toHResult(error: HError): HResult[T] = {
        opt match {
          case Some(value) => Right(value)
          case None => Result.error(error)
        }
      }
    }
  }

  trait HError

  case class ClientError(message: String) extends HError

  case class InternalError[T <: Throwable](exception: T, reason: Option[String] = None) extends HError

  def error[T](err: HError): HResult[T] = Left(err)

  def clientError[T](message: String): HResult[T] = error(ClientError(message))

  def internalError[T <: Throwable](ex: T): HResult[T] = error(InternalError(ex, None))

  def internalError[T <: Throwable](ex: T, reason: String): HResult[T] = error(InternalError(ex, Some(reason)))

  def errorF[T](err: HError): HFResult[T] = Future.successful(error(err))

  def clientErrorF[T](message: String): HFResult[T] = errorF(ClientError(message))

  def internalErrorF[T <: Throwable](ex: T): HFResult[T] = errorF(InternalError(ex, None))

  def internalErrorF[T <: Throwable](ex: T, reason: String): HFResult[T] = errorF(InternalError(ex, Some(reason)))
}