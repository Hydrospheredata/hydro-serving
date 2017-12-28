package io.hydrosphere.serving.manager.controller

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import io.hydrosphere.serving.manager.connector.{ExecutionFailure, ExecutionSuccess}
import io.hydrosphere.serving.manager.connector.{ExecutionFailure, ExecutionResult, ExecutionSuccess}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util._

trait ServingDataDirectives extends Logging {

  def extractRawData: Directive1[Array[Byte]] =
    withoutSizeLimit & extractStrictEntity(10.seconds).map(e => e.data.toArray)

  def completeExecutionResult(f: Future[ExecutionResult]): Route = {
    onComplete(f)({
      case Success(result: ExecutionSuccess) =>
        complete(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, result.json)))
      case Success(failure: ExecutionFailure) =>
        complete(
          HttpResponse(
            status = failure.statusCode,
            entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, failure.error)
        ))
      case Failure(err) =>
        logger.error("Serving failed", err)
        complete(
          HttpResponse(
            status = StatusCodes.InternalServerError,
            entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, s"Serving failed ${err.getMessage}")
          ))
    })
  }
}

object ServingDataDirectives extends ServingDataDirectives
