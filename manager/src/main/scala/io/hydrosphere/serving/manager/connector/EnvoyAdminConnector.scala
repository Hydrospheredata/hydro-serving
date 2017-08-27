package io.hydrosphere.serving.manager.connector

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future

trait EnvoyAdminConnector {

  def stats(host: String, port: Int): Future[String]

}

class HttpEnvoyAdminConnector(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer
) extends EnvoyAdminConnector with Logging {

  implicit val executionContext = system.dispatcher

  override def stats(host: String, port: Int): Future[String] = {
    val flow = Http(system).outgoingConnection(host, port)
      .mapAsync(1) { r =>
        if (r.status != StatusCodes.OK) {
          logger.debug(s"Wrong status from service ${r.status}")
        }
        Unmarshal(r.entity).to[String]
      }

    val source = Source.single(HttpRequest(
      uri = "/stats",
      method = HttpMethods.GET
    ))
    source.via(flow).runWith(Sink.head)
  }
}
