package io.hydrosphere.serving.manager.controller

import java.nio.file.{Files, Path, StandardOpenOption}

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{as, complete, entity, onComplete}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import io.hydrosphere.serving.manager.controller.model.{Entities, ModelDeploy}
import io.hydrosphere.serving.manager.model.{HFResult, HResult, Result}
import io.hydrosphere.serving.manager.model.Result.{HError, InternalError}
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
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

  final def getFileWithMeta[T: JsonReader, R: ToResponseMarshaller](callback: (Option[Path], Option[T]) => HFResult[R])
    (implicit mat: Materializer, ec: ExecutionContext) = {
    entity(as[Multipart.FormData]) { formdata =>

      val fileSource = formdata.parts
        .filter(part => part.filename.isDefined && part.name == Entities.payload)
        .flatMapConcat { part =>
          val filename = part.filename.get
          val tempPath = Files.createTempFile("payload", filename)
          part.entity.dataBytes
            .map { fileBytes =>
              Files.write(tempPath, fileBytes.toArray, StandardOpenOption.APPEND)
              tempPath
            }
        }
        .take(1)

      val filePartF = fileSource.runWith(Sink.headOption)

      val metadataSource = formdata.parts
        .filter(part => part.name == Entities.metadata)
        .flatMapConcat { part =>
          part.entity.dataBytes
            .map(_.decodeString("UTF-8"))
            .filter(_.isEmpty)
            .map(_.parseJson.convertTo[T])
        }
        .take(1)

      val metadataPart = metadataSource.runWith(Sink.headOption)

      completeFRes(
        filePartF.zip(metadataPart).flatMap{
          case (file, meta) => callback(file, meta)
        }
      )
    }
  }

  def routes: Route
}