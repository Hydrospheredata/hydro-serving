package io.hydrosphere.serving.manager.model

import spray.json.{JsObject, JsString, JsValue, JsonWriter}
import spray.json._

import scala.concurrent.Future

object Result {
  trait HError

  case class ClientError(message: String) extends HError

  object ClientError {
    implicit val clientErrorFormat = new JsonWriter[ClientError] {
      override def write(obj: ClientError): JsValue = {
        JsObject(Map(
          "error" -> JsString(obj.message)
        ))
      }
    }
  }

  case class InternalError[T <: Throwable](exception: T, reason: Option[String] = None) extends HError

  object InternalError {
    implicit def internalErrorFormat[T <: Throwable] = new JsonWriter[InternalError[T]] {
      override def write(obj: InternalError[T]): JsValue = {
        val fields = Map(
          "exception" -> JsString(obj.exception.getMessage)
        )
        val reasonField = obj.reason.map { r =>
          Map("reason" -> JsString(r))
        }.getOrElse(Map.empty)

        JsObject(fields ++ reasonField)
      }
    }
  }

  object HError {
    implicit val errorFormat: JsonWriter[HError] = new JsonWriter[HError] {
      override def write(obj: HError): JsValue = {
        obj match {
          case x: ClientError => JsObject(Map(
            "error" -> JsString("Client"),
            "information" -> x.toJson
          ))
          case x: InternalError[_] => JsObject(Map(
            "error" -> JsString("Internal"),
            "information" -> x.toJson
          ))
        }
      }
    }
  }

  def clientError[T](message: String): HResult[T] = Left(ClientError(message))
  def internalError[T <: Throwable](ex: T): HResult[T] = Left(InternalError(ex, None))
  def internalError[T <: Throwable](ex: T, reason: String): HResult[T] = Left(InternalError(ex, Some(reason)))

  def clientErrorF[T](message: String): HFResult[T] = Future.successful(Left(ClientError(message)))
  def internalErrorF[T <: Throwable](ex: T): HFResult[T] = Future.successful(Left(InternalError(ex, None)))
  def internalErrorF[T <: Throwable](ex: T, reason: String): HFResult[T] = Future.successful(Left(InternalError(ex, Some(reason))))
}