package io.hydrosphere.serving.gateway

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import io.hydrosphere.serving.connector.{HttpRuntimeMeshConnector, RuntimeMeshConnector}
import io.hydrosphere.serving.gateway.actor.{PipelineSynchronizeActor, ServeActor}
import io.hydrosphere.serving.gateway.connector.{HttpManagerConnector, ManagerConnector}

/**
  *
  */
class GatewayActors(config: GatewayConfiguration)(implicit val system: ActorSystem,
                                                  implicit val materializer: ActorMaterializer) {

  implicit val sidecar = config.sidecar
  val sidecarConnector: RuntimeMeshConnector = new HttpRuntimeMeshConnector()

  val managerConnector: ManagerConnector = new HttpManagerConnector(config)

  val serveActor: ActorRef = system.actorOf(Props(classOf[ServeActor], sidecarConnector),
    "serveActor")

  val pipelineSynchronizeActor: ActorRef = system.actorOf(
    Props(classOf[PipelineSynchronizeActor], managerConnector, serveActor),
    "pipelineSynchronizeActor")

}
