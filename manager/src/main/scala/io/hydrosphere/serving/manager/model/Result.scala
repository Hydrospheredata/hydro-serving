package io.hydrosphere.serving.manager.model

import scala.concurrent.{ExecutionContext, Future}

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

  def ok[T](t: T): HResult[T] = Right(t)

  def okF[T](t: T): HFResult[T] = Future.successful(Right(t))

  trait HError {
    def message: String
  }

  case class ClientError(message: String) extends HError

  case class InternalError[T <: Throwable](exception: T, reason: Option[String] = None) extends HError {
    override def message: String = exception.getMessage
  }

  case class ErrorCollection(errors: Seq[HError]) extends HError {
    override def message: String = errors.map(_.message).mkString("\n")
  }

  def error[T](err: HError): HResult[T] = Left(err)

  def errors[T](errors: Seq[HError]): HResult[T] = error(ErrorCollection(errors))

  def clientError[T](message: String): HResult[T] = error(ClientError(message))

  def internalError[T, E <: Throwable](ex: E): HResult[T] = error(InternalError(ex, None))

  def internalError[T, E <: Throwable](ex: E, reason: String): HResult[T] = error(InternalError(ex, Some(reason)))

  def errorF[T](err: HError): HFResult[T] = Future.successful(error(err))

  def errorsF[T](errors: Seq[HError]): HFResult[T] = errorF(ErrorCollection(errors))

  def clientErrorF[T](message: String): HFResult[T] = errorF(ClientError(message))

  def internalErrorF[T, E <: Throwable](ex: E): HFResult[T] = errorF(InternalError(ex, None))

  def internalErrorF[T, E <: Throwable](ex: E, reason: String): HFResult[T] = errorF(InternalError(ex, Some(reason)))

  def sequence[T](reSeq: Seq[HResult[T]]): HResult[Seq[T]] = {
    val errors = reSeq.filter(_.isLeft).map(_.left.get)
    if (errors.nonEmpty) {
      Result.errors(errors)
    } else {
      val values = reSeq.filter(_.isRight).map(_.right.get)
      Result.ok(values)
    }
  }

  def sequenceF[T](reSeq: Seq[HFResult[T]])(implicit ec: ExecutionContext): HFResult[Seq[T]] = {
    Future.sequence(reSeq).map(sequence)
  }

  def traverse[A, T](seq: Seq[A])(func: A => HResult[T]): HResult[Seq[T]] = {
    sequence(seq.map(func))
  }

  def traverseF[A, T](seq: Seq[A])(func: A => HFResult[T])(implicit ec: ExecutionContext): HFResult[Seq[T]] = {
    Future.traverse(seq)(func).map(sequence)
  }
}