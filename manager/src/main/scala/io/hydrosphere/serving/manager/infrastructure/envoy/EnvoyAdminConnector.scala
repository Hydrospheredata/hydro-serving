package io.hydrosphere.serving.manager.infrastructure.envoy

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import cats.effect.Async
import io.hydrosphere.serving.manager.util.AsyncUtil
import org.apache.logging.log4j.scala.Logging

trait EnvoyAdminConnector[F[_]] {
  def stats(host: String, port: Int): F[String]
}

object EnvoyAdminConnector {
  def http[F[_]: Async](implicit system: ActorSystem, mat: ActorMaterializer): EnvoyAdminConnector[F] =
    new EnvoyAdminConnector[F] with Logging {
      implicit val executionContext = system.dispatcher

      override def stats(host: String, port: Int): F[String] = {
        AsyncUtil.futureAsync {
          val flow = Http(system).outgoingConnection(host, port)
            .mapAsync(1) { r =>
              if (r.status != StatusCodes.OK) {
                logger.debug(s"Wrong status from service ${r.status}")
              }
              Unmarshal(r.entity).to[String]
            }

          val source = Source.single(HttpRequest(
            uri = "/stats?format=prometheus",
            method = HttpMethods.GET
          ))
          source.via(flow).runWith(Sink.head)
        }
      }
    }
}