package io.hydrosphere.serving.manager.controller

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import io.hydrosphere.serving.manager.model.{HFResult, HResult}
import io.hydrosphere.serving.manager.model.Result.{HError, InternalError}
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.util.{Failure, Success}

trait GenericController extends Logging {
  final def completeRes[T](res: HResult[T])(implicit responseMarshaller: ToResponseMarshaller[T]): Route = {
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

  final def completeFRes[T](res: HFResult[T])(implicit responseMarshaller: ToResponseMarshaller[T]): Route = {
    onComplete(res){
      case Success(result) =>
        completeRes(result)
      case Failure(err) =>
        logger.error("Future failed", err)
        val error: HError = InternalError(err, Some("Future failed"))
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