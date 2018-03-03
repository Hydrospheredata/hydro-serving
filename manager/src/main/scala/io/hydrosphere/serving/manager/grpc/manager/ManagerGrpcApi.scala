package io.hydrosphere.serving.manager.grpc.manager

import io.hydrosphere.serving.manager.ManagerServices
import io.hydrosphere.serving.tensorflow.api.predict.{PredictRequest, PredictResponse}
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc.PredictionService
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}

class ManagerGrpcApi(
  managerServices: ManagerServices,
  grpcClient: PredictionServiceGrpc.PredictionServiceStub
)(implicit ec: ExecutionContext) extends PredictionService with Logging {

  override def predict(request: PredictRequest): Future[PredictResponse] = {
    request.modelSpec match {
      case Some(modelSpec) =>
        managerServices.applicationManagementService.serveGrpcApplication(request)

      case None => Future.failed(new IllegalArgumentException("ModelSpec is not defined"))
    }
  }
}
