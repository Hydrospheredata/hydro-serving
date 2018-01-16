package io.hydrosphere.serving.manager.service.envoy.xds

import envoy.api.v2.RouteAction.ClusterSpecifier
import envoy.api.v2._
import io.grpc.stub.StreamObserver

/**
  *
  */
class RouteDSActor extends AbstractDSActor[RouteConfiguration](typeUrl = "type.googleapis.com/envoy.api.v2.RouteConfiguration") {

  private def createRoute(name: String): RouteConfiguration =
    RouteConfiguration(
      name = name,
      virtualHosts = Seq(VirtualHost(
        name = "all",
        domains = Seq("*"),
        routes = Seq(Route(
          `match` = Some(RouteMatch(
            pathSpecifier = RouteMatch.PathSpecifier.Prefix("/")
          )),
          action = Route.Action.Route(RouteAction(
            clusterSpecifier = ClusterSpecifier.Cluster("manager")
          ))
        ))
      ))
    )

  override protected def formResources(responseObserver: StreamObserver[DiscoveryResponse]): Seq[RouteConfiguration] =
    Seq(createRoute("mesh"))
}
