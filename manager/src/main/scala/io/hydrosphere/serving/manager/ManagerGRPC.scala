package io.hydrosphere.serving.manager

import envoy.service.discovery.v2.AggregatedDiscoveryServiceGrpc
import io.hydrosphere.serving.grpc.BuilderWrapper
import io.hydrosphere.serving.manager.grpc.envoy.AggregatedDiscoveryServiceGrpcImpl
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc

import scala.concurrent.ExecutionContext

class ManagerGRPC
(
  managerServices: ManagerServices,
  managerConfiguration: ManagerConfiguration
)(
  implicit val ex: ExecutionContext
) {

  val aggregatedDiscoveryServiceGrpc = new AggregatedDiscoveryServiceGrpcImpl(managerServices.envoyGRPCDiscoveryService)

  val builder = BuilderWrapper(io.grpc.ServerBuilder.forPort(managerConfiguration.application.grpcPort))
    .addService(AggregatedDiscoveryServiceGrpc.bindService(aggregatedDiscoveryServiceGrpc, ex))
  val server = builder.build

  server.start()
}
