package io.hydrosphere.serving.gateway.connector

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpRequest, RequestEntity}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.hydrosphere.serving.gateway.GatewayConfiguration
import io.hydrosphere.serving.model.CommonJsonSupport

import scala.concurrent.Future

/**
  *
  */
case class SingleExecutionCommand(headers: Seq[HttpHeader],
                                  json: Seq[Any],
                                  runtimeName: String,
                                  runtimePath: String)

case class ExecutionResult(headers: Seq[HttpHeader],
                           json: Seq[Any])

trait SidecarConnector {
  def execute(command: SingleExecutionCommand): Future[ExecutionResult]
}

class HttpSidecarConnector(config: GatewayConfiguration)
                          (implicit val system: ActorSystem,
                           implicit val materializer: ActorMaterializer) extends SidecarConnector with CommonJsonSupport {
  implicit val executionContext = system.dispatcher

  val http = Http(system)

  override def execute(command: SingleExecutionCommand): Future[ExecutionResult] = {
    Marshal(command.json).to[RequestEntity].flatMap(entity => {
      val source = Source.single(HttpRequest(method = HttpMethods.POST,
        uri = s"http://${command.runtimeName}/${command.runtimePath}",
        entity = entity,
        headers = collection.immutable.Seq(command.headers: _*)))

      val flow = http.outgoingConnection(config.sidecar.host, config.sidecar.port)
        .mapAsync(1) { r =>
          Unmarshal(r.entity).to[Seq[Any]]
            .map(ExecutionResult(r.headers, _))
        }

      source.via(flow).runWith(Sink.head)
    })
  }
}
