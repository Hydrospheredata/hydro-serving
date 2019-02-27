package io.hydrosphere.serving.manager.infrastructure.envoy

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import cats.effect.Effect
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.hydrosphere.serving.manager.domain.application.{Application, ApplicationRepository}
import io.hydrosphere.serving.manager.domain.clouddriver.{CloudDriver, CloudService}
import io.hydrosphere.serving.manager.domain.servable.ServableService
import io.hydrosphere.serving.manager.infrastructure.envoy.events.DiscoveryEvent._
import io.hydrosphere.serving.manager.infrastructure.envoy.xds._

import scala.util.control.NonFatal

class XDSManagementActor[F[_]: Effect](
  cloudDriver: CloudDriver[F],
  serviceManagementService: ServableService[F],
  appRepo: ApplicationRepository[F],
  clusterDSActor: ActorRef,
  endpointDSActor: ActorRef,
  listenerDSActor: ActorRef,
  routeDSActor: ActorRef,
  applicationDSActor: ActorRef
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

  def initClusters(services: Seq[CloudService]) = Effect[F].delay {
    val serviceNames = services.map(_.serviceName).toSet
    clusterDSActor ! SyncCluster(serviceNames)
  }
  

  def initEndpoints(services: Seq[CloudService]) = Effect[F].delay{
    endpointDSActor ! RenewEndpoints(mapCloudService(services))
  }

  def initApps(applications: Seq[Application]) = Effect[F].delay {
    val m = SyncApplications(applications)
    routeDSActor ! m
    applicationDSActor ! m
  }

  override def preStart(): Unit = {
    log.info("XDS actor prestart")

    val f = for {
      cloudServices <- cloudDriver.serviceList()
      applications <- appRepo.all()
      _ <- initClusters(cloudServices)
      _ <- initApps(applications)
      _ <- initEndpoints(cloudServices)
    } yield {
      log.info("XDS initialized")
    }

    val handled = Effect[F].onError(f) {
      case NonFatal(x) =>
        Effect[F].pure {
          log.error(s"XDS initialization error: $x")
        }
    }

    Effect[F].toIO(handled).unsafeRunAsyncAndForget()

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
  def props[F[_] : Effect](
    cloudDriver: CloudDriver[F],
    servableService: ServableService[F],
    appRepository: ApplicationRepository[F],
    clusterDSActor: ActorRef,
    endpointDSActor: ActorRef,
    listenerDSActor: ActorRef,
    routeDSActor: ActorRef,
    applicationDSActor: ActorRef
  ): Props = {
    Props(new XDSManagementActor[F](
      cloudDriver = cloudDriver,
      serviceManagementService = servableService,
      appRepo = appRepository,
      clusterDSActor = clusterDSActor,
      endpointDSActor = endpointDSActor,
      listenerDSActor = listenerDSActor,
      routeDSActor = routeDSActor,
      applicationDSActor = applicationDSActor
    ))
  }

  def makeXdsActor[F[_] : Effect](
    cloudDriver: CloudDriver[F],
    servableService: ServableService[F],
    applicationRepository: ApplicationRepository[F])
    (implicit actorSystem: ActorSystem) = {
    val clusterDSActor: ActorRef = actorSystem.actorOf(Props[ClusterDSActor])
    val endpointDSActor: ActorRef = actorSystem.actorOf(Props[EndpointDSActor])
    val listenerDSActor: ActorRef = actorSystem.actorOf(Props[ListenerDSActor])
    val routeDSActor: ActorRef = actorSystem.actorOf(Props[RouteDSActor])
    val applicationDSActor: ActorRef = actorSystem.actorOf(Props[ApplicationDSActor])

    val xdsManagementActor: ActorRef = actorSystem.actorOf(XDSManagementActor.props(
      cloudDriver = cloudDriver,
      servableService = servableService,
      appRepository = applicationRepository,
      clusterDSActor = clusterDSActor,
      endpointDSActor = endpointDSActor,
      listenerDSActor = listenerDSActor,
      routeDSActor = routeDSActor,
      applicationDSActor = applicationDSActor
    ))

    xdsManagementActor
  }
}