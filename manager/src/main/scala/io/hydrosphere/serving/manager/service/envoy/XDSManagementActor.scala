package io.hydrosphere.serving.manager.service.envoy

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.hydrosphere.serving.manager.service.application.ApplicationManagementService
import io.hydrosphere.serving.manager.service.clouddriver.{CloudDriverService, CloudService}
import io.hydrosphere.serving.manager.service.envoy.xds._
import io.hydrosphere.serving.manager.service.internal_events._
import io.hydrosphere.serving.manager.service.service.ServiceManagementService

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class XDSManagementActor(
  serviceManagementService: ServiceManagementService,
  applicationManagementService: ApplicationManagementService,
  cloudDriverService: CloudDriverService,
  clusterDSActor: ActorRef,
  endpointDSActor: ActorRef,
  listenerDSActor: ActorRef,
  routeDSActor: ActorRef,
  applicationDSActor: ActorRef
) extends Actor with ActorLogging {
  import context._

  context.system.eventStream.subscribe(self, classOf[ApplicationChanged])
  context.system.eventStream.subscribe(self, classOf[ApplicationRemoved])
  context.system.eventStream.subscribe(self, classOf[ServiceChanged])
  context.system.eventStream.subscribe(self, classOf[ServiceRemoved])
  context.system.eventStream.subscribe(self, classOf[CloudServiceDetected])

  private val actors = Map(
    "type.googleapis.com/envoy.api.v2.Cluster" -> clusterDSActor,
    "type.googleapis.com/envoy.api.v2.ClusterLoadAssignment" -> endpointDSActor,
    "type.googleapis.com/envoy.api.v2.RouteConfiguration" -> routeDSActor,
    "type.googleapis.com/envoy.api.v2.Listener" -> listenerDSActor,
    "type.googleapis.com/io.hydrosphere.serving.manager.grpc.applications.Application" -> applicationDSActor
  )

  log.info(s"Expected XDS typeUrls: ${actors.keys}")

  override def receive: Receive = {
    case u: UnsubscribeMsg =>
      log.debug(u.toString)
      actors.values.foreach(_ ! u)
    case s: SubscribeMsg =>
      log.debug(s.toString)
      s.discoveryRequest.node.foreach { _ =>
        actors.get(s.discoveryRequest.typeUrl)
          .fold(log.error(s"Unknown typeUrl: ${s.discoveryRequest}"))(_ ! s)
      }
    case app: ApplicationChanged =>
      log.debug(app.toString)
      routeDSActor ! app
      applicationDSActor ! app
    case app: ApplicationRemoved =>
      log.debug(app.toString)
      routeDSActor ! app
      applicationDSActor ! app
    case c: CloudServiceDetected =>
      log.debug(c.toString)
      endpointDSActor ! AddEndpoints(mapCloudService(c.cloudServices))
    case s: ServiceChanged =>
      log.debug(s.toString)
      clusterDSActor ! AddCluster(Set(s.service.serviceName))
    case s: ServiceRemoved =>
      log.debug(s.toString)
      val set = Set(s.service.serviceName)
      clusterDSActor ! RemoveClusters(set)
      endpointDSActor ! RemoveEndpoints(set)
  }

  override def preStart(): Unit = {
    val f1 = serviceManagementService.allServices()
      .map(v => clusterDSActor ! SyncCluster(v.map(_.serviceName).toSet))

    val f2 = applicationManagementService.allApplications()
      .map { v =>
        val m = SyncApplications(v)
        routeDSActor ! m
        applicationDSActor ! m
      }

    val f3 = cloudDriverService.serviceList()
      .map(c => endpointDSActor ! RenewEndpoints(mapCloudService(c)))

    val f = Future.sequence(List(f1,f2,f3))

    val awaitTime = 30.seconds
    log.info(s"Waiting $awaitTime for XDS subactors init")
    Await.result(f, awaitTime)
    log.info("Subactors initialized")

    super.preStart()
  }

  private def mapCloudService(c: Seq[CloudService]): Seq[ClusterInfo] =
    c.map { t =>
      ClusterInfo(
        name = t.serviceName,
        endpoints = t.instances.map { i =>
          ClusterEndpoint(
            host = i.mainApplication.host,
            port = i.mainApplication.port,
            advertisedHost = i.advertisedHost,
            advertisedPort = i.advertisedPort
          )
        }.toSet
      )
    }
}

object XDSManagementActor {
  def props(
    serviceManagementService: ServiceManagementService,
    applicationManagementService: ApplicationManagementService,
    cloudDriverService: CloudDriverService,
    clusterDSActor: ActorRef,
    endpointDSActor: ActorRef,
    listenerDSActor: ActorRef,
    routeDSActor: ActorRef,
    applicationDSActor: ActorRef
  ): Props = {
    Props(classOf[XDSManagementActor],
      serviceManagementService,
      applicationManagementService,
      cloudDriverService,
      clusterDSActor,
      endpointDSActor,
      listenerDSActor,
      routeDSActor,
      applicationDSActor
    )
  }
}