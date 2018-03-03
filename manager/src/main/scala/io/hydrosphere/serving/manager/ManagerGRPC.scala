package io.hydrosphere.serving.manager

import envoy.api.v2._
import io.hydrosphere.serving.grpc.BuilderWrapper
import io.hydrosphere.serving.manager.grpc.envoy.AggregatedDiscoveryServiceGrpcImpl
import io.hydrosphere.serving.manager.grpc.manager.ManagerGrpcApi
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
  val managerGrpcApi = new ManagerGrpcApi(managerServices, managerServices.servingMeshGrpcClient)

  val builder = BuilderWrapper(io.grpc.ServerBuilder.forPort(managerConfiguration.application.grpcPort))
    .addService(AggregatedDiscoveryServiceGrpc.bindService(aggregatedDiscoveryServiceGrpc, ex))
    .addService(PredictionServiceGrpc.bindService(managerGrpcApi, ex))
  val server = builder.build

  server.start()
}
