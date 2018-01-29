package io.hydrosphere.serving.manager

import envoy.api.v2._
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}
import io.hydrosphere.serving.manager.grpc.envoy.AggregatedDiscoveryServiceGrpcImpl
import io.hydrosphere.serving.manager.grpc.manager.ManagerGrpcApi
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc

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

  case class BuilderWrapper[T <: ServerBuilder[T]](builder: ServerBuilder[T]) {
    def addService(service: ServerServiceDefinition): BuilderWrapper[T] = {
      BuilderWrapper(builder.addService(service))
    }

    def build: Server = {
      builder.build()
    }
  }

  val aggregatedDiscoveryServiceGrpc = new AggregatedDiscoveryServiceGrpcImpl(managerServices.envoyGRPCDiscoveryService)
  val managerGrpcApi = new ManagerGrpcApi(managerServices, managerServices.servingMeshGrpcClient)

  val builder = BuilderWrapper(ServerBuilder.forPort(managerConfiguration.application.grpcPort))
    .addService(AggregatedDiscoveryServiceGrpc.bindService(aggregatedDiscoveryServiceGrpc, ex))
    .addService(PredictionServiceGrpc.bindService(managerGrpcApi, ex))
  val server = builder.build

  server.start()
}

