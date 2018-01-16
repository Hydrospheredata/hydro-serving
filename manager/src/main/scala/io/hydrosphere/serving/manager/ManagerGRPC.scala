package io.hydrosphere.serving.manager

import envoy.api.v2._
import io.grpc.ServerBuilder
import io.hydrosphere.serving.manager.grpc.envoy.AggregatedDiscoveryServiceGrpcImpl

import scala.concurrent.ExecutionContext

/**
  *
  */
class ManagerGRPC
(
  managerServices: ManagerServices,
  managerConfiguration: ManagerConfiguration
)(
  implicit val ex: ExecutionContext
) {

  val aggregatedDiscoveryServiceGrpc = new AggregatedDiscoveryServiceGrpcImpl(managerServices.envoyGRPCDiscoveryService)


  val server = ServerBuilder.forPort(managerConfiguration.application.grpcPort)
    .addService(AggregatedDiscoveryServiceGrpc.bindService(aggregatedDiscoveryServiceGrpc, ex))
    .build()
    //.addService(ClusterDiscoveryServiceGrpc.bindService(separateDiscoveryServiceGrpcImpl, ex)).

    //.addService()
    //.addService(ListenerDiscoveryServiceGrpc.bindService(separateDiscoveryServiceGrpcImpl, ex))
    //.addService(EndpointDiscoveryServiceGrpc.bindService(separateDiscoveryServiceGrpcImpl, ex))
    //.addService(RouteDiscoveryServiceGrpc.bindService(separateDiscoveryServiceGrpcImpl, ex))

  server.start()
}
