package io.hydrosphere.serving.manager.service.envoy

import akka.actor.{ActorRef, ActorSystem, Props}
import envoy.api.v2.{DiscoveryRequest, DiscoveryResponse}
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.service.envoy.xds._
import org.apache.logging.log4j.scala.Logging

trait EnvoyGRPCDiscoveryService {
  def subscribe(discoveryRequest: DiscoveryRequest, responseObserver: StreamObserver[DiscoveryResponse]): Unit

  def unsubscribe(responseObserver: StreamObserver[DiscoveryResponse]): Unit
}

class EnvoyGRPCDiscoveryServiceImpl
(
  implicit val system: ActorSystem
) extends EnvoyGRPCDiscoveryService with Logging {

  private val clusterDSActor: ActorRef = system.actorOf(Props(new ClusterDSActor))

  private val endpointDSActor: ActorRef = system.actorOf(Props(new EndpointDSActor))

  private val listenerDSActor: ActorRef = system.actorOf(Props(new ListenerDSActor))

  private val routeDSActor: ActorRef = system.actorOf(Props(new RouteDSActor))

  private val actors = Map(
    "type.googleapis.com/envoy.api.v2.Cluster" -> clusterDSActor,
    "type.googleapis.com/envoy.api.v2.ClusterLoadAssignment" -> endpointDSActor,
    "type.googleapis.com/envoy.api.v2.RouteConfiguration" -> routeDSActor,
    "type.googleapis.com/envoy.api.v2.Listener" -> listenerDSActor
  )



  override def subscribe(discoveryRequest: DiscoveryRequest, responseObserver: StreamObserver[DiscoveryResponse]): Unit = {
    discoveryRequest.node.foreach(n => {
      actors.get(discoveryRequest.typeUrl)
        .fold(logger.info(s"Unknown typeUrl: $discoveryRequest"))(actor => {

          actor ! SubscribeMsg(
            discoveryRequest = discoveryRequest,
            responseObserver = responseObserver
          )
        })
    })

  }

  override def unsubscribe(responseObserver: StreamObserver[DiscoveryResponse]): Unit = {
    val msg = UnsubscribeMsg(responseObserver)
    actors.values.foreach(actor => {
      actor ! msg
    })


    clusterDSActor ! ClusterAdded(Set("manager"))
    endpointDSActor ! RenewEndpoints(Seq(ClusterInfo(
      name = "manager",
      endpoints = Set(ClusterEndpoint(
        host = "192.168.90.68",
        port = 9090
      ))
    )))
  }
}