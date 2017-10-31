package io.hydrosphere.serving.manager.controller

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util._

trait RawDataDirectives {

  def extractRawData: Directive1[Array[Byte]] =
    withoutSizeLimit & extractStrictEntity(5.seconds).map(e => e.data.toArray)

  def completeRawData(f: Future[Array[Byte]]): Route = {
    onComplete(f)({
      case Success(data) =>
        complete(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, data)))
      case Failure(err) =>
        //todo - use 500?
        complete(
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Serving failed ${err.getMessage}")
          ))
    })
  }
}

object RawDataDirectives extends RawDataDirectives
