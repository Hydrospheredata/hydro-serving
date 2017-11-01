package io.hydrosphere.serving.connector

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{StatusCode, _}
import akka.http.scaladsl.model.headers.Host
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.hydrosphere.serving.config.SidecarConfig
import io.hydrosphere.serving.model.CommonJsonSupport
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class ExecutionUnit(
  serviceName: String,
  servicePath: String
)

case class ExecutionCommand(
  headers: Seq[HttpHeader],
  json: Array[Byte],
  pipe: Seq[ExecutionUnit]
)

sealed trait ExecutionResult
case class ExecutionSuccess(json: Array[Byte]) extends ExecutionResult
case class ExecutionFailure(error: String, status: StatusCode) extends ExecutionResult

trait RuntimeMeshConnector {
  def execute(command: ExecutionCommand): Future[ExecutionResult]
}

class HttpRuntimeMeshConnector(config: SidecarConfig)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends RuntimeMeshConnector with CommonJsonSupport with Logging {
  implicit val executionContext = system.dispatcher

  //TODO ConnectionPool
  //TODO Move to streams
  val flow = Http(system).outgoingConnection(config.host, config.port)
    .mapAsync(1) { r =>
      r.entity.toStrict(10.seconds).map(entity => {
        r.status match {
          case StatusCodes.OK => ExecutionSuccess(entity.data.toArray)
          case errCode =>
            ExecutionFailure(s"Response code ${errCode.intValue()}" + new String(entity.data.toArray), errCode)
        }
      })
    }

  private def execute(runtimeName: String, runtimePath: String, headers: Seq[HttpHeader], json: Array[Byte]): Future[ExecutionResult] = {
    val source = Source.single(HttpRequest(
      uri = runtimePath,
      method = HttpMethods.POST,
      entity = HttpEntity.apply(ContentTypes.`application/json`, json),
      headers = collection.immutable.Seq(headers: _*) :+ Host.apply(runtimeName)
    ))
    source.via(flow).runWith(Sink.head)
  }

  override def execute(command: ExecutionCommand): Future[ExecutionResult] = {
    val accum: Future[ExecutionResult] = Future.successful(ExecutionSuccess(command.json))
    command.pipe.foldLeft(accum) { case (acc, item) =>
      acc.flatMap({
        case ExecutionSuccess(data) =>
          execute(item.serviceName, item.servicePath, command.headers, data)
        case failure: ExecutionFailure =>
          Future.successful(failure)
      })
    }
  }
}
