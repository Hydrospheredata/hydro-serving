package io.hydrosphere.serving.manager.api.grpc

import cats.effect.Effect
import envoy.service.discovery.v2.AggregatedDiscoveryServiceGrpc
import io.hydrosphere.serving.grpc.BuilderWrapper
import io.hydrosphere.serving.manager.api.ManagerServiceGrpc
import io.hydrosphere.serving.manager.{ManagerRepositories, ManagerServices}
import io.hydrosphere.serving.manager.config.ManagerConfiguration

import scala.concurrent.ExecutionContext

object GrpcApiServer {
  def apply[F[_]: Effect](
    managerRepositories: ManagerRepositories[F],
    managerServices: ManagerServices[F],
    managerConfiguration: ManagerConfiguration
  )(implicit ex: ExecutionContext) = {
    val aggregatedDiscoveryServiceGrpc = new AggregatedDiscoveryService(managerServices.envoyGRPCDiscoveryService)
    val managerGrpcService = new ManagerGrpcService(managerRepositories.modelVersionRepository)

    val builder = BuilderWrapper(io.grpc.ServerBuilder.forPort(managerConfiguration.application.grpcPort))
      .addService(AggregatedDiscoveryServiceGrpc.bindService(aggregatedDiscoveryServiceGrpc, ex))
      .addService(ManagerServiceGrpc.bindService(managerGrpcService, ex))

    builder.build
  }
}
