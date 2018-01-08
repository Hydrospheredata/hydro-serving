package io.hydrosphere.serving.manager

import envoy.api.v2.AggregatedDiscoveryServiceGrpc
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
  implicit val ex: ExecutionContext //TODO change to thread pool
) {

  val aggregatedDiscoveryServiceGrpc=new AggregatedDiscoveryServiceGrpcImpl

  val server = ServerBuilder.forPort(managerConfiguration.application.grpcPort)
    .addService(AggregatedDiscoveryServiceGrpc.bindService(aggregatedDiscoveryServiceGrpc, ex))
    .build()
  server.start()
}
