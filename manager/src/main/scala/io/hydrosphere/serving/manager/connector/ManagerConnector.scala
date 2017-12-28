package io.hydrosphere.serving.manager.connector

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.hydrosphere.serving.manager.controller.CommonJsonSupport
import io.hydrosphere.serving.manager.model.Application

import scala.concurrent.Future

/**
  *
  */
trait ManagerConnector {

  def getApplications: Future[Seq[Application]]
}

class HttpManagerConnector(host: String, port: Int = 80)
                          (implicit val system: ActorSystem,
                           implicit val materializer: ActorMaterializer) extends ManagerConnector with CommonJsonSupport {

  val http = Http(system)

  override def getApplications: Future[Seq[Application]] = {
    val source = Source.single(HttpRequest(uri = Uri(path = Path("/api/v1/applications"))))
    val flow = http.outgoingConnection(host, port)
      .mapAsync(1) { r =>
        Unmarshal(r.entity).to[Seq[Application]]
      }
    source.via(flow).runWith(Sink.head)
  }
}