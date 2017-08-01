package io.hydrosphere.serving.gateway.connector

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.hydrosphere.serving.gateway.GatewayConfiguration
import io.hydrosphere.serving.model.{CommonJsonSupport, Endpoint}

import scala.concurrent.Future

/**
  *
  */
trait ManagerConnector {
  def getEndpoints: Future[Seq[Endpoint]]
}

class HttpManagerConnector(config: GatewayConfiguration)
                          (implicit val system: ActorSystem,
                           implicit val materializer: ActorMaterializer) extends ManagerConnector with CommonJsonSupport {

  val http = Http(system)

  override def getEndpoints: Future[Seq[Endpoint]] = {
    val source = Source.single(HttpRequest(uri = Uri(path = Path("/api/v1/endpoints"))))
    val flow = http.outgoingConnection(config.manager.host, config.manager.port)
      .mapAsync(1) { r =>
        Unmarshal(r.entity).to[Seq[Endpoint]]
      }
    source.via(flow).runWith(Sink.head)
  }

}