package io.hydrosphere.serving.manager.service.envoy.xds

import envoy.api.v2.RouteAction.ClusterSpecifier
import envoy.api.v2._
import io.grpc.stub.StreamObserver

/**
  *
  */
class RouteDSActor extends AbstractDSActor {

  private def createRoute(name: String): RouteConfiguration =
    RouteConfiguration(
      name = name,
      virtualHosts = Seq(VirtualHost(
        name = name,
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

  /*
   virtual_hosts:
    - name: local_service
      domains: ["*"]
      routes:
      - match: { prefix: "/" }
        route: { cluster: some_service }
  */


  private def sendRoutes(stream: StreamObserver[DiscoveryResponse]): Unit = {
    val list = Seq(createRoute("ingress"), createRoute("egress"))

    send(DiscoveryResponse(
      typeUrl = "type.googleapis.com/envoy.api.v2.RouteConfiguration",
      versionInfo = "0",
      resources = list.map(s => com.google.protobuf.any.Any.pack(s))
    ), stream)
  }

  override def receive: Receive = {
    case subscribe: SubscribeMsg =>
      observers += subscribe.responseObserver
      sendRoutes(subscribe.responseObserver)

    case unsubcribe: UnsubscribeMsg =>
      observers -= unsubcribe.responseObserver

  }
}
