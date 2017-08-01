package io.hydrosphere.serving.connector

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Host
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.hydrosphere.serving.config.SidecarConfig
import io.hydrosphere.serving.model.CommonJsonSupport
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future

case class ExecutionUnit(
  serviceName: String,
  servicePath: String
)

case class ExecutionCommand(
  headers: Seq[HttpHeader],
  json: Seq[Any],
  pipe: Seq[ExecutionUnit]
)

case class ExecutionResult(
  headers: Seq[HttpHeader],
  json: Seq[Any]
)

trait RuntimeMeshConnector {
  def execute(command: ExecutionCommand): Future[ExecutionResult]
}

class HttpRuntimeMeshConnector(
  config: SidecarConfig
)(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends RuntimeMeshConnector with CommonJsonSupport with Logging {
  implicit val executionContext = system.dispatcher

  //TODO ConnectionPool
  //TODO Move to streams
  //TODO avoid Serialization/Deserialization, work with http entry
  val flow = Http(system).outgoingConnection(config.host, config.port)
    .mapAsync(1) { r =>
      if (r.status != StatusCodes.OK) {
        logger.debug(s"Wrong status from service ${r.status}")
      }
      Unmarshal(r.entity).to[Seq[Any]]
        .map(ExecutionResult(r.headers, _))
    }

  private def execute(runtimeName: String, runtimePath: String, headers: Seq[HttpHeader], json: Seq[Any]): Future[ExecutionResult] = {
    Marshal(json).to[RequestEntity].flatMap(entity => {
      val source = Source.single(HttpRequest(
        uri = runtimePath,
        method = HttpMethods.POST,
        entity = entity,
        headers = collection.immutable.Seq(headers: _*) :+ Host.apply(runtimeName)
      ))
      source.via(flow).runWith(Sink.head)
    })
  }

  override def execute(command: ExecutionCommand): Future[ExecutionResult] = {
    val executionUnit=command.pipe.head
    var fAccum = execute(executionUnit.serviceName, executionUnit.servicePath, command.headers, command.json)
    for(item <- command.pipe.drop(1)) {
      fAccum = fAccum.flatMap(res=>{
        execute(item.serviceName, item.servicePath, command.headers, res.json)
      })
    }
    fAccum
  }
}
