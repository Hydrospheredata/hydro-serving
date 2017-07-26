package io.hydrosphere.serving.gateway

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.gateway.actor.{PipelineSynchronizeActor, ServeActor}
import io.hydrosphere.serving.gateway.connector.{HttpManagerConnector, HttpSidecarConnector, ManagerConnector, SidecarConnector}

/**
  *
  */
class GatewayActors(config: GatewayConfiguration)(implicit val system: ActorSystem,
                                                  implicit val materializer: ActorMaterializer) {

  val sidecarConnector: SidecarConnector = new HttpSidecarConnector(config)

  val managerConnector: ManagerConnector = new HttpManagerConnector(config)

  val serveActor: ActorRef = system.actorOf(Props(classOf[ServeActor], sidecarConnector),
    "serveActor")

  val pipelineSynchronizeActor: ActorRef = system.actorOf(
    Props(classOf[PipelineSynchronizeActor], managerConnector, serveActor),
    "pipelineSynchronizeActor")

}
