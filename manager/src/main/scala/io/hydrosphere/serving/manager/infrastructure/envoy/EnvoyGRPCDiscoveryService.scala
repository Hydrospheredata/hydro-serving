package io.hydrosphere.serving.manager.infrastructure.envoy

import akka.actor.{ActorRef, ActorSystem, Props}
import envoy.api.v2.{DiscoveryRequest, DiscoveryResponse}
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.config.ManagerConfiguration
import io.hydrosphere.serving.manager.domain.application.ApplicationService
import io.hydrosphere.serving.manager.domain.service.ServiceManagementService
import io.hydrosphere.serving.manager.infrastructure.envoy.xds._
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

trait EnvoyGRPCDiscoveryService {
  def subscribe(discoveryRequest: DiscoveryRequest, responseObserver: StreamObserver[DiscoveryResponse]): Unit

  def unsubscribe(responseObserver: StreamObserver[DiscoveryResponse]): Unit
}

class EnvoyGRPCDiscoveryServiceImpl(
  serviceManagementService: ServiceManagementService,
  applicationManagementService: ApplicationService,
  managerConfiguration: ManagerConfiguration
)(
  implicit val ex: ExecutionContext,
  actorSystem: ActorSystem
) extends EnvoyGRPCDiscoveryService with Logging {

  logger.info("Creating envoy actors")
  private val clusterDSActor: ActorRef = actorSystem.actorOf(Props[ClusterDSActor])
  //TODO use managerConfiguration for endpoint configuration
  private val endpointDSActor: ActorRef = actorSystem.actorOf(Props[EndpointDSActor])
  private val listenerDSActor: ActorRef = actorSystem.actorOf(Props[ListenerDSActor])
  private val routeDSActor: ActorRef = actorSystem.actorOf(Props[RouteDSActor])
  private val applicationDSActor: ActorRef = actorSystem.actorOf(Props[ApplicationDSActor])

  logger.info("Creating XDS management actor")
  private val xdsManagementActor: ActorRef = actorSystem.actorOf(XDSManagementActor.props(
    serviceManagementService,
    applicationManagementService,
    clusterDSActor,
    endpointDSActor,
    listenerDSActor,
    routeDSActor,
    applicationDSActor
  ))

  override def subscribe(discoveryRequest: DiscoveryRequest, responseObserver: StreamObserver[DiscoveryResponse]): Unit =
    discoveryRequest.node.foreach { _ =>
      xdsManagementActor ! SubscribeMsg(
        discoveryRequest = discoveryRequest,
        responseObserver = responseObserver
      )
    }

  override def unsubscribe(responseObserver: StreamObserver[DiscoveryResponse]): Unit = {
    val msg = UnsubscribeMsg(responseObserver)
    xdsManagementActor ! msg
  }
}