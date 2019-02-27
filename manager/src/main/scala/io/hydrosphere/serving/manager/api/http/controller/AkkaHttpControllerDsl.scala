package io.hydrosphere.serving.manager.api.http.controller

import java.nio.file.{Files, Path}

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import cats.effect.Effect
import cats.syntax.flatMap._
import io.hydrosphere.serving.manager.domain.DomainError
import io.hydrosphere.serving.manager.domain.DomainError.{InternalError, InvalidRequest, NotFound}
import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol
import io.hydrosphere.serving.manager.util.AsyncUtil
import org.apache.logging.log4j.scala.Logging
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait AkkaHttpControllerDsl extends CompleteJsonProtocol with Logging {

  import AkkaHttpControllerDsl._

  final def getFileWithMeta[F[_] : Effect, T: JsonReader, R: ToResponseMarshaller](callback: (Option[Path], Option[T]) => F[Either[DomainError, R]])
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

      val entitiesF: F[List[UploadE]] = AsyncUtil.futureAsync {
        parts.runFold(List.empty[UploadE]) {
          case (a, b) => a :+ b
        }
      }

      completeFRes {
        entitiesF.flatMap { entities =>
          val file = entities.find(_.isInstanceOf[UploadFile]).map(_.asInstanceOf[UploadFile].path)
          val metadata = entities.find(_.isInstanceOf[UploadMeta]).map(_.asInstanceOf[UploadMeta].meta.convertTo[T])
          callback(file, metadata)
        }
      }
    }
  }

  final def withF[F[_] : Effect, T: ToResponseMarshaller](res: F[T])(f: T => Route): Route = {
    onComplete(Effect[F].toIO(res).unsafeToFuture()) {
      case Success(result) =>
        f(result)
      case Failure(err) => commonExceptionHandler(err)
    }
  }

  final def completeF[F[_] : Effect, T: ToResponseMarshaller](res: F[T]): Route  = {
    withF(res)(complete(_))
  }

  final def completeRes[T: ToResponseMarshaller](res: Either[DomainError, T]): Route  = {
    res match {
      case Left(a) =>
        a match {
          case NotFound(_) =>
            complete(
              HttpResponse(
                status = StatusCodes.NotFound,
                entity = HttpEntity(ContentTypes.`application/json`, a.toJson.toString)
              )
            )
          case InvalidRequest(_) =>
            complete(
              HttpResponse(
                status = StatusCodes.BadRequest,
                entity = HttpEntity(ContentTypes.`application/json`, a.toJson.toString)
              )
            )
          case InternalError(err) =>
            logger.error(err)
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

  final def completeFRes[F[_] : Effect, T: ToResponseMarshaller](res: F[Either[DomainError, T]]): Route

  = {
    withF(res)(completeRes(_))
  }

  final def commonExceptionHandler = ExceptionHandler {
    case DeserializationException(msg, _, fields) =>
      logger.error(msg)
      complete(
        HttpResponse(
          StatusCodes.BadRequest,
          entity = Map(
            "error" -> "RequestDeserializationError",
            "message" -> msg,
            "fields" -> fields
          ).asInstanceOf[Map[String, Any]].toJson.toString()
        )
      )
    case p: SerializationException =>
      logger.error(p.getMessage, p)
      complete(
        HttpResponse(
          StatusCodes.InternalServerError,
          entity = Map(
            "error" -> "ResponseSerializationException",
            "message" -> Option(p.getMessage).getOrElse(s"Unknown error: $p")
          ).toJson.toString()
        )
      )
    case p: Throwable =>
      logger.error(p.toString)
      logger.error(p.getStackTrace.mkString("\n"))
      complete(
        HttpResponse(
          StatusCodes.InternalServerError,
          entity = Map(
            "error" -> "InternalException",
            "message" -> Option(p.toString).getOrElse(s"Unknown error: $p")
          ).toJson.toString()
        )
      )
  }
}

object AkkaHttpControllerDsl {

  sealed trait UploadE

  case class UploadFile(path: Path) extends UploadE

  case class UploadMeta(meta: JsValue) extends UploadE

  case class UploadUnknown(key: String, filename: Option[String]) extends UploadE

}