package io.hydrosphere.serving.gateway.connector

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Host
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import io.hydrosphere.serving.gateway.GatewayConfiguration
import io.hydrosphere.serving.model.CommonJsonSupport
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{Future, Promise}

/**
  *
  */
case class ExecutionUnit(
  runtimeName: String,
  runtimePath: String
)

case class SingleExecutionCommand(
  headers: Seq[HttpHeader],
  json: Seq[Any],
  runtimeName: String,
  runtimePath: String
)

case class ExecutionCommand(
  headers: Seq[HttpHeader],
  json: Seq[Any],
  pipe: Seq[ExecutionUnit]
)

case class ExecutionResult(headers: Seq[HttpHeader],
  json: Seq[Any])

trait SidecarConnector {
  def execute(command: SingleExecutionCommand): Future[ExecutionResult]
}

class HttpSidecarConnector(
  config: GatewayConfiguration
)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends SidecarConnector with CommonJsonSupport with Logging {
  implicit val executionContext = system.dispatcher

  //TODO ConnectionPool
  val flow = Http(system).outgoingConnection(config.sidecar.host, config.sidecar.port)
    .mapAsync(1) { r =>
      if (r.status != StatusCodes.OK) {
        logger.debug(s"Wrong status from service ${r.status}")
      }
      Unmarshal(r.entity).to[Seq[Any]]
        .map(ExecutionResult(r.headers, _))
    }

  override def execute(command: SingleExecutionCommand): Future[ExecutionResult] = {
    Marshal(command.json).to[RequestEntity].flatMap(entity => {
      val source = Source.single(HttpRequest(
        method = HttpMethods.POST,
        entity = entity,
        headers = collection.immutable.Seq(command.headers: _*) :+ Host.apply(command.runtimeName)
      ))
      source.via(flow).runWith(Sink.head)
    })
  }
}
