package io.hydrosphere.serving.manager.api.grpc

import envoy.service.discovery.v2.AggregatedDiscoveryServiceGrpc
import io.hydrosphere.serving.grpc.BuilderWrapper
import io.hydrosphere.serving.manager.ManagerServices
import io.hydrosphere.serving.manager.config.ManagerConfiguration

import scala.concurrent.ExecutionContext

object GrpcApiServer {
  def apply(
    managerServices: ManagerServices,
    managerConfiguration: ManagerConfiguration
  )(implicit ex: ExecutionContext) = {
    val aggregatedDiscoveryServiceGrpc = new AggregatedDiscoveryServiceGrpcImpl(managerServices.envoyGRPCDiscoveryService)

    val builder = BuilderWrapper(io.grpc.ServerBuilder.forPort(managerConfiguration.application.grpcPort))
      .addService(AggregatedDiscoveryServiceGrpc.bindService(aggregatedDiscoveryServiceGrpc, ex))

    builder.build
  }
}
