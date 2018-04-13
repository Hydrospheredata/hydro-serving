package io.hydrosphere.serving.manager.controller

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import io.hydrosphere.serving.manager.model.{HFResult, HResult}
import io.hydrosphere.serving.manager.model.Result.{HError, InternalError}
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.concurrent.Future
import scala.util.{Failure, Success}


trait GenericController extends CompleteJsonProtocol with Logging {
  final def withF[T: ToResponseMarshaller](res: Future[T])(f: T => Route): Route = {
    onComplete(res){
      case Success(result) =>
        f(result)
      case Failure(err) =>
        logger.error("Future failed", err)
        val error: HError = InternalError(err, Some("Future failed"))
        complete(
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(ContentTypes.`application/json`, error.toJson.toString)
          )
        )
    }
  }

  final def completeF[T: ToResponseMarshaller](res: Future[T]): Route = {
    withF(res)(complete(_))
  }

  final def completeRes[T: ToResponseMarshaller](res: HResult[T]): Route = {
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

  final def completeFRes[T: ToResponseMarshaller](res: HFResult[T]): Route = {
    withF(res)(completeRes(_))
  }

  def routes: Route
}