package io.hydrosphere.serving.manager.connector

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Host
import akka.http.scaladsl.model.{StatusCode, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.hydrosphere.serving.manager.SidecarConfig
import io.hydrosphere.serving.manager.model.CommonJsonSupport
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future
import scala.concurrent.duration._

case class ExecutionUnit(
  serviceName: String,
  servicePath: String
)

case class ExecutionCommand(
  headers: Seq[HttpHeader],
  json: Array[Byte],
  pipe: Seq[ExecutionUnit]
)

sealed trait ExecutionResult {
  val statusCode: StatusCode
}
case class ExecutionSuccess(
  json: Array[Byte],
  statusCode: StatusCode
) extends ExecutionResult

case class ExecutionFailure(
  error: String,
  statusCode: StatusCode
) extends ExecutionResult

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
  //TODO HARDCODE senb inner requests into localhost:9292(special envoy listener) to build correct traces for zipkin
  val flow = Http(system).outgoingConnection("localhost", 9292)
    .mapAsync(1) { r =>
      r.entity.toStrict(10.seconds).map(entity => {
        val data = entity.data.toArray
        r.status match {
          case StatusCodes.OK => ExecutionSuccess(data, r.status)
          case errCode =>
            val msg = s"Response code ${errCode.intValue()} " + new String(data)
            ExecutionFailure(msg, r.status)
        }
      })
    }

  private def execute(runtimeName: String, runtimePath: String, headers: Seq[HttpHeader], json: Array[Byte]): Future[ExecutionResult] = {
    val headers2 = collection.immutable.Seq(headers: _*)
    val httpRequest=HttpRequest(
      uri = runtimePath,
      method = HttpMethods.POST,
      entity = HttpEntity.apply(ContentTypes.`application/json`, json),
      headers = headers2 :+ Host.apply(runtimeName)
    )
    val source = Source.single(httpRequest)
    source.via(flow).runWith(Sink.head)
  }

  override def execute(command: ExecutionCommand): Future[ExecutionResult] = {
    val empty = ExecutionSuccess(
      command.json,
      StatusCodes.OK
    )

    val accum: Future[ExecutionResult] = Future.successful(empty)

    command.pipe.foldLeft(accum) { case (acc, item) =>
      acc.flatMap({
        case success: ExecutionSuccess =>
          execute(
            item.serviceName,
            item.servicePath,
            command.headers,
            success.json
          )

        case failure: ExecutionFailure =>
          Future.successful(failure)
      })
    }
  }
}
