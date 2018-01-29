package io.hydrosphere.serving.manager.service.envoy

import akka.actor.{Actor, ActorLogging, ActorRef}
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.clouddriver.{CloudDriverService, CloudService}
import io.hydrosphere.serving.manager.service.envoy.xds._

import scala.concurrent.ExecutionContext

class XDSManagementActor(
  serviceManagementService: ServiceManagementService,
  applicationManagementService: ApplicationManagementService,
  cloudDriverService: CloudDriverService,
  clusterDSActor: ActorRef,
  endpointDSActor: ActorRef,
  listenerDSActor: ActorRef,
  routeDSActor: ActorRef
)(
  implicit val ex: ExecutionContext
) extends Actor with ActorLogging {

  context.system.eventStream.subscribe(self, classOf[ApplicationChanged])
  context.system.eventStream.subscribe(self, classOf[ApplicationRemoved])
  context.system.eventStream.subscribe(self, classOf[ServiceChanged])
  context.system.eventStream.subscribe(self, classOf[ServiceRemoved])
  context.system.eventStream.subscribe(self, classOf[CloudServiceDetected])

  private val actors = Map(
    "type.googleapis.com/envoy.api.v2.Cluster" -> clusterDSActor,
    "type.googleapis.com/envoy.api.v2.ClusterLoadAssignment" -> endpointDSActor,
    "type.googleapis.com/envoy.api.v2.RouteConfiguration" -> routeDSActor,
    "type.googleapis.com/envoy.api.v2.Listener" -> listenerDSActor
  )

  override def receive: Receive = {
    case u: UnsubscribeMsg =>
      actors.values.foreach(actor => {
        actor ! u
      })
    case s: SubscribeMsg =>
      s.discoveryRequest.node.foreach(_ => {
        actors.get(s.discoveryRequest.typeUrl)
          .fold(log.info(s"Unknown typeUrl: ${s.discoveryRequest}"))(actor =>
            actor ! s
          )
      })
    case app: ApplicationChanged =>
      routeDSActor ! app
    case app: ApplicationRemoved =>
      routeDSActor ! app
    case c: CloudServiceDetected =>
      endpointDSActor ! AddEndpoints(mapCloudService(c.cloudServices))
    case s: ServiceChanged =>
      clusterDSActor ! AddCluster(Set(s.service.serviceName))
    case s: ServiceRemoved =>
      val set = Set(s.service.serviceName)
      clusterDSActor ! RemoveClusters(set)
      endpointDSActor ! RemoveEndpoints(set)
  }

  override def preStart(): Unit = {
    serviceManagementService.allServices()
      .foreach(v => clusterDSActor ! AddCluster(v.map(_.serviceName).toSet))

    applicationManagementService.allApplications()
      .foreach(v => routeDSActor ! SyncApplications(v))

    cloudDriverService.serviceList()
      .foreach(c => endpointDSActor ! AddEndpoints(mapCloudService(c)))
    super.preStart()
  }

  private def mapCloudService(c: Seq[CloudService]): Seq[ClusterInfo] =
    c.map(t => {
      ClusterInfo(
        name = t.serviceName,
        endpoints = t.instances.map(i => {
          ClusterEndpoint(
            host = i.advertisedHost,
            port = i.advertisedPort
          )
        }).toSet
      )
    })

}
