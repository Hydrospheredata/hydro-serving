package io.hydrosphere.serving.manager.infrastructure.envoy.xds

import com.google.protobuf.duration.Duration
import envoy.api.v2.core.{HeaderValue, HeaderValueOption}
import envoy.api.v2.route.RouteAction.ClusterSpecifier
import envoy.api.v2.route.{Route, _}
import envoy.api.v2.{DiscoveryResponse, RouteConfiguration}
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.grpc.Headers
import io.hydrosphere.serving.manager.domain.application.Application
import io.hydrosphere.serving.manager.domain.clouddriver.DefaultConstants._
import io.hydrosphere.serving.manager.infrastructure.envoy.events.DiscoveryEvent._

import scala.collection.mutable

case class SyncApplications(applications: Seq[Application])

class RouteDSActor extends AbstractDSActor[RouteConfiguration](typeUrl = "type.googleapis.com/envoy.api.v2.RouteConfiguration") {

  private val applications = mutable.Map[Long, Seq[VirtualHost]]()

  private val routeTimeout = Some(Duration(42069,0))

  private val kafkaGatewayHost = createSystemHost(GATEWAY_KAFKA_NAME)
  private val monitoringHost = createSystemHost(MONITORING_NAME)
  private val profilerHost = createSystemHost(PROFILER_NAME)
  private val gatewayHost = createSystemHost(GATEWAY_HTTP_NAME)

  private def createRoutes(application: Application): Seq[VirtualHost] =
    application.executionGraph.stages.zipWithIndex.map { case (appStage, i) =>
      val weights = ClusterSpecifier.WeightedClusters(WeightedCluster(
        clusters = appStage.modelVariants.map(w => {
          WeightedCluster.ClusterWeight(
            name = "mv" + w.modelVersion.id.toString, // TODO it's not ok. need to refactor to use Service.name
            weight = Some(w.weight),
            responseHeadersToAdd = Seq(HeaderValueOption(
              header = Some(HeaderValue(
                key = Headers.XServingModelVersionId.name,
                value = w.modelVersion.id.toString)),
              append = Some(true)
            ))
          )
        })
      ))
      //TODO generate unique ID
      createVirtualHost(s"app${application.id}stage$i", weights)
    }

  private def createVirtualHost(name: String, weights: ClusterSpecifier.WeightedClusters): VirtualHost =
    VirtualHost(
      name = name,
      domains = Seq(name),
      routes = Seq(Route(
        `match` = Some(RouteMatch(
          pathSpecifier = RouteMatch.PathSpecifier.Prefix("/")
        )),
        action = Route.Action.Route(RouteAction(
          clusterSpecifier = weights,
          timeout = routeTimeout
        ))
      ))
    )

  private def addOrUpdateApplication(application: Application): Unit =
    applications.put(application.id, createRoutes(application))


  private def removeApplications(ids: Set[Long]): Set[Boolean] =
    ids.map(id => applications.remove(id).nonEmpty)


  private def renewApplications(apps: Seq[Application]): Unit = {
    applications.clear()
    apps.foreach(addOrUpdateApplication)
  }

  override def receiveStoreChangeEvents(mes: Any): Boolean =
    mes match {
      case a: SyncApplications =>
        renewApplications(a.applications)
        true
      case a: ApplicationChanged =>
        addOrUpdateApplication(a.application)
        true
      case a: ApplicationRemoved =>
        removeApplications(Set(a.application.id))
          .contains(true)
      case _ => false
    }

  private def createRoute(name: String, defaultRoute: VirtualHost): RouteConfiguration =
    RouteConfiguration(
      name = name,
      virtualHosts = applications.values.flatten.toSeq
        :+ defaultRoute :+ kafkaGatewayHost :+ monitoringHost
        :+ profilerHost :+ gatewayHost
    )

  private def createSystemHost(name: String): VirtualHost =
    VirtualHost(
      name = name,
      domains = Seq(name),
      routes = Seq(Route(
        `match` = Some(RouteMatch(
          pathSpecifier = RouteMatch.PathSpecifier.Prefix("/")
        )),
        action = Route.Action.Route(RouteAction(
          clusterSpecifier = ClusterSpecifier.Cluster(name),
          timeout = routeTimeout
        ))
      ))
    )

  private def defaultVirtualHost(clusterName: String): VirtualHost =
    VirtualHost(
      name = "all",
      domains = Seq("*"),
      routes = Seq(Route(
        `match` = Some(RouteMatch(
          pathSpecifier = RouteMatch.PathSpecifier.Prefix("/")
        )),
        action = Route.Action.Route(RouteAction(
          clusterSpecifier = ClusterSpecifier.Cluster(clusterName)
        ))
      ))
    )

  private def defaultManagerVirtualHost(): VirtualHost =
    VirtualHost(
      name = "all",
      domains = Seq("*"),
      routes = Seq(
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/tensorflow.serving.PredictionService"),
            headers = Seq(HeaderMatcher(
              name = "content-type",
              value = "application/grpc"
            ))
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(GATEWAY_NAME),
            timeout = routeTimeout
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/"),
            headers = Seq(HeaderMatcher(
              name = "content-type",
              value = "application/grpc"
            ))
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(MANAGER_NAME)
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Path("/health")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(MANAGER_HTTP_NAME)
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/api-docs")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(MANAGER_HTTP_NAME)
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/swagger")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(MANAGER_HTTP_NAME)
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/api/v1/applications/serve")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(GATEWAY_HTTP_NAME),
            timeout = routeTimeout
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/api")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(MANAGER_HTTP_NAME),
            timeout = routeTimeout
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/monitoring")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(MONITORING_HTTP_NAME),
            timeout = routeTimeout
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/profiler")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(PROFILER_HTTP_NAME),
            timeout = routeTimeout
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/gateway")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(GATEWAY_HTTP_NAME),
            timeout = routeTimeout
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(MANAGER_UI_NAME),
            timeout = routeTimeout
          ))
        )
      )
    )

  override protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[RouteConfiguration] = {
    val clusterName = getObserverNode(responseObserver).fold("manager_xds_cluster")(_.id)

    val defaultRoute = clusterName match {
      case MANAGER_NAME =>
        defaultManagerVirtualHost()
      case _ =>
        defaultVirtualHost(clusterName)
    }

    Seq(createRoute(ROUTE_CONFIG_NAME, defaultRoute))
  }
}
