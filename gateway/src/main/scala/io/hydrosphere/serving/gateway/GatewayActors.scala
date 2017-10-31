package io.hydrosphere.serving.gateway

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.connector.{HttpRuntimeMeshConnector, ManagerConnector, RuntimeMeshConnector}
import io.hydrosphere.serving.gateway.actor.{PipelineSynchronizeActor, ServeActor}
import io.hydrosphere.serving.connector.HttpManagerConnector

/**
  *
  */
class GatewayActors(config: GatewayConfiguration)(implicit val system: ActorSystem,
                                                  implicit val materializer: ActorMaterializer) {

  val sidecarConnector: RuntimeMeshConnector = new HttpRuntimeMeshConnector(config.sidecar)

  val managerConnector: ManagerConnector = new HttpManagerConnector(config.manager.host, config.manager.port)

  val serveActor: ActorRef = system.actorOf(Props(classOf[ServeActor], sidecarConnector),
    "serveActor")

  val pipelineSynchronizeActor: ActorRef = system.actorOf(
    Props(classOf[PipelineSynchronizeActor], managerConnector, serveActor),
    "pipelineSynchronizeActor")

}
