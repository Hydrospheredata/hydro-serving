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

case class ExecutionUnit(
  serviceName: String,
  servicePath: String
)

case class ExecutionCommand(
  headers: Seq[HttpHeader],
  json: Array[Byte],
  pipe: Seq[ExecutionUnit]
)

case class ExecutionResult(
  headers: Seq[HttpHeader],
  json: Array[Byte],
  status: StatusCode
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

      r.entity.toStrict(5.seconds)
        .map(entity => ExecutionResult(r.headers, entity.data.toArray, r.status))
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

  //TODO: should we stop call rest services if first failed?
  override def execute(command: ExecutionCommand): Future[ExecutionResult] = {
    val executionUnit = command.pipe.head
    var fAccum = execute(executionUnit.serviceName, executionUnit.servicePath, command.headers, command.json)
    for (item <- command.pipe.drop(1)) {
      fAccum = fAccum.flatMap(res => {
        execute(item.serviceName, item.servicePath, command.headers, res.json)
      })
    }
    fAccum
  }
}
