package io.hydrosphere.serving.manager.service.envoy.xds

import envoy.api.v2.core.{HeaderValue, HeaderValueOption}
import envoy.api.v2.{DiscoveryResponse, RouteConfiguration}
import envoy.api.v2.route.RouteAction.ClusterSpecifier
import envoy.api.v2.route._
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.grpc.Headers
import io.hydrosphere.serving.manager.model.db.Application
import io.hydrosphere.serving.manager.service.clouddriver.CloudDriverService
import io.hydrosphere.serving.manager.service.internal_events.{ApplicationChanged, ApplicationRemoved}

import scala.collection.mutable

case class SyncApplications(applications: Seq[Application])

class RouteDSActor extends AbstractDSActor[RouteConfiguration](typeUrl = "type.googleapis.com/envoy.api.v2.RouteConfiguration") {

  private val applications = mutable.Map[Long, Seq[VirtualHost]]()

  private val kafkaGatewayHost = createGatewayHost(CloudDriverService.GATEWAY_KAFKA_NAME)

  private def createRoutes(application: Application): Seq[VirtualHost] =
    application.executionGraph.stages.zipWithIndex.map { case (appStage, i) =>
      val weights = ClusterSpecifier.WeightedClusters(WeightedCluster(
        clusters = appStage.services.map(w => {
          WeightedCluster.ClusterWeight(
            name = w.serviceDescription.toServiceName(),
            weight = Some(w.weight),
            responseHeadersToAdd = Seq(HeaderValueOption(
              header = Some(HeaderValue(
                key = Headers.XServingModelVersionId.name,
                value = w.serviceDescription.modelVersionId.map(_.toString).getOrElse(""))),
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
          clusterSpecifier = weights
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
      virtualHosts = applications.values.flatten.toSeq :+ defaultRoute :+ kafkaGatewayHost
    )

  private def createGatewayHost(name: String): VirtualHost =
    VirtualHost(
      name = name,
      domains = Seq(name),
      routes = Seq(Route(
        `match` = Some(RouteMatch(
          pathSpecifier = RouteMatch.PathSpecifier.Prefix("/")
        )),
        action = Route.Action.Route(RouteAction(
          clusterSpecifier = ClusterSpecifier.Cluster(name)
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
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/"),
            headers = Seq(HeaderMatcher(
              name = "content-type",
              value = "application/grpc"
            ))
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(CloudDriverService.MANAGER_NAME)
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Path("/health")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(CloudDriverService.MANAGER_HTTP_NAME)
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/api-docs")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(CloudDriverService.MANAGER_HTTP_NAME)
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/swagger")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(CloudDriverService.MANAGER_HTTP_NAME)
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/api")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(CloudDriverService.MANAGER_HTTP_NAME)
          ))
        ),
        Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster(CloudDriverService.MANAGER_UI_NAME)
          ))
        )
      )
    )

  override protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[RouteConfiguration] = {
    val clusterName = getObserverNode(responseObserver).fold("manager_xds_cluster")(_.id)

    val defaultRoute = clusterName match {
      case CloudDriverService.MANAGER_NAME =>
        defaultManagerVirtualHost()
      case _ =>
        defaultVirtualHost(clusterName)
    }

    Seq(createRoute(ROUTE_CONFIG_NAME, defaultRoute))
  }
}
