package io.hydrosphere.serving.gateway.actor

import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import io.hydrosphere.serving.gateway.connector.SidecarConnector
import io.hydrosphere.serving.model.Endpoint
import io.hydrosphere.serving.controller.TracingHeaders


case class UpdateEndpoints(endpoints: Map[String, Endpoint])

class ServeActor(sidecarConnector: SidecarConnector) extends Actor with ActorLogging {

  var endpoints: Map[String, Endpoint] = Map()

  override def receive = {
    case UpdateEndpoints(indexed) => this.endpoints = indexed

    case httpRequest: HttpRequest =>
      //Store tracing headers
      val tracingHeaders = httpRequest.headers
        .filter(h => TracingHeaders.isTracingHeaderName(h.name()))
      //TODO
      sender() ! HttpResponse(entity = "actor responds nicely", headers = tracingHeaders)
  }
}
