package io.hydrosphere.serving.manager.api.grpc

import com.google.protobuf.empty.Empty
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.api.GetVersionRequest
import io.hydrosphere.serving.manager.api.ManagerServiceGrpc.ManagerService
import io.hydrosphere.serving.manager.domain.model_version.ModelVersionRepositoryAlgebra
import io.hydrosphere.serving.manager.grpc.entities.ModelVersion
import io.hydrosphere.serving.manager.infrastructure.envoy.Converters

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ManagerGrpcService(versionRepositoryAlgebra: ModelVersionRepositoryAlgebra[Future])(implicit ec: ExecutionContext) extends ManagerService {
  override def getAllVersions(request: Empty, responseObserver: StreamObserver[ModelVersion]): Unit = {
    versionRepositoryAlgebra.all()
      .onComplete {
      case Success(versions) =>
        println("SUCCESS")
        versions.foreach { x =>
          println("AAAA")
          responseObserver.onNext(Converters.grpcModelVersion(x))
        }
        responseObserver.onCompleted()
      case Failure(exception) =>
        println("FAILURE")
        responseObserver.onError(exception)
    }
  }

  override def getVersion(request: GetVersionRequest): Future[ModelVersion] = {
    versionRepositoryAlgebra.get(request.id).flatMap {
      case Some(version) => Future.successful(Converters.grpcModelVersion(version))
      case None => Future.failed(new IllegalArgumentException(s"ModelVersion with id ${request.id} is not found"))
    }
  }
}