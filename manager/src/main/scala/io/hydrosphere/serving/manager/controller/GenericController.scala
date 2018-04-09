package io.hydrosphere.serving.manager.controller

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import io.hydrosphere.serving.manager.controller.GenericController.{FResult, HSError, InternalError, Result}
import org.apache.logging.log4j.scala.Logging
import spray.json.{JsObject, JsString, JsValue, JsonWriter, _}

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait GenericController extends Logging {
  final def completeRes[T](res: Result[T])(implicit responseMarshaller: ToResponseMarshaller[T]): Route = {
    res match {
      case Left(a) =>
        complete(
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(ContentTypes.`application/json`, a.toJson.toString)
          )
        )
      case Right(b) => complete(b)
    }
  }

  final def completeFRes[T](res: FResult[T])(implicit responseMarshaller: ToResponseMarshaller[T]): Route = {
    onComplete(res){
      case Success(result) =>
        completeRes(result)
      case Failure(err) =>
        logger.error("Future failed", err)
        val error: HSError = InternalError(err, Some("Future failed"))
        complete(
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, error.toJson.toString)
          )
        )
    }
  }

  def routes: Route
}

object GenericController {
  trait HSError

  case class ClientError(message: String) extends HSError

  object ClientError {
    implicit val clientErrorFormat = new JsonWriter[ClientError] {
      override def write(obj: ClientError): JsValue = {
        JsObject(Map(
          "error" -> JsString(obj.message)
        ))
      }
    }
  }

  case class InternalError[T <: Throwable](exception: T, reason: Option[String] = None) extends HSError

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

  object HSError {
    implicit val errorFormat: JsonWriter[HSError] = new JsonWriter[HSError] {
      override def write(obj: HSError): JsValue = {
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

  type Result[T] = Either[HSError, T]
  type FResult[T] = Future[Either[HSError, T]]
}