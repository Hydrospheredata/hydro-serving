package io.hydrosphere.serving.manager.api.http.controller

import java.nio.file.{Files, Path}

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server._
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol
import io.hydrosphere.serving.model.api.Result.{ClientError, HError, InternalError}
import io.hydrosphere.serving.model.api.{HFResult, HResult}
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

trait GenericController extends CompleteJsonProtocol with Logging {

  import GenericController._

  final def getFileWithMeta[T: JsonReader, R: ToResponseMarshaller](callback: (Option[Path], Option[T]) => HFResult[R])
    (implicit mat: Materializer, ec: ExecutionContext) = {
    entity(as[Multipart.FormData]) { formdata =>
      val parts = formdata.parts.mapAsync(2) { part =>
        logger.debug(s"Got part ${part.name} filename=${part.filename}")
        part.name match {
          case "payload" if part.filename.isDefined =>
            val filename = part.filename.get
            val tempPath = Files.createTempFile("payload", filename)
            part.entity.dataBytes
              .runWith(FileIO.toPath(tempPath))
              .map(_ => UploadFile(tempPath))

          case "metadata" if part.filename.isEmpty =>
            part.toStrict(5.minutes).map { k =>
              UploadMeta(k.entity.data.utf8String.parseJson)
            }

          case x =>
            logger.warn(s"Got unknown part name=${part.name} filename=${part.filename}")
            Future.successful(UploadUnknown(x, part.filename))
        }
      }

      val entitiesF = parts.runFold(List.empty[UploadE]) {
        case (a, b) => a :+ b
      }

      completeFRes {
        entitiesF.flatMap { entities =>
          logger.debug(s"Upload entities: $entities")
          val file = entities.find(_.isInstanceOf[UploadFile]).map(_.asInstanceOf[UploadFile].path)
          val metadata = entities.find(_.isInstanceOf[UploadMeta]).map(_.asInstanceOf[UploadMeta].meta.convertTo[T])
          callback(file, metadata)
        }
      }
    }
  }

  final def withF[T: ToResponseMarshaller](res: Future[T])(f: T => Route): Route = {
    onComplete(res) {
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
        a match {
          case ClientError(_) =>
            complete(
              HttpResponse(
                status = StatusCodes.BadRequest,
                entity = HttpEntity(ContentTypes.`application/json`, a.toJson.toString)
              )
            )
          case InternalError(_, _) =>
            complete(
              HttpResponse(
                status = StatusCodes.InternalServerError,
                entity = HttpEntity(ContentTypes.`application/json`, a.toJson.toString)
              )
            )
        }
      case Right(b) => complete(b)
    }
  }

  final def completeFRes[T: ToResponseMarshaller](res: HFResult[T]): Route = {
    withF(res)(completeRes(_))
  }

  def routes: Route
}

object GenericController {
  sealed trait UploadE

  case class UploadFile(path: Path) extends UploadE

  case class UploadMeta(meta: JsValue) extends UploadE

  case class UploadUnknown(key: String, filename: Option[String]) extends UploadE
}