package io.hydrosphere.serving.manager.api.grpc

import cats.data.EitherT
import cats.effect.Effect
import cats.syntax.applicativeError._
import cats.syntax.functor._
import com.google.protobuf.empty.Empty
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.manager.api.GetVersionRequest
import io.hydrosphere.serving.manager.api.ManagerServiceGrpc.ManagerService
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersionRepository, ModelVersion => DMV}
import io.hydrosphere.serving.manager.grpc.entities.ModelVersion
import io.hydrosphere.serving.manager.infrastructure.envoy.Converters

import scala.concurrent.Future

class ManagerGrpcService[F[_]: Effect](versionRepositoryAlgebra: ModelVersionRepository[F]) extends ManagerService {
  override def getAllVersions(request: Empty, responseObserver: StreamObserver[ModelVersion]): Unit = {
    val fAction = versionRepositoryAlgebra.all().map { versions =>
      versions.foreach { v =>
        responseObserver.onNext(Converters.grpcModelVersion(v))
      }
      responseObserver.onCompleted()
    }.onError {
      case exception =>
        Effect[F].delay {
          responseObserver.onError(exception)
        }
    }

    Effect[F].toIO(fAction).unsafeRunAsyncAndForget()
  }

  override def getVersion(request: GetVersionRequest): Future[ModelVersion] = {
    val f = for {
      version <- EitherT.fromOptionF[F, Throwable, DMV](versionRepositoryAlgebra.get(request.id), new IllegalArgumentException(s"ModelVersion with id ${request.id} is not found"))
    } yield Converters.grpcModelVersion(version)

    Effect[F].toIO(
      Effect[F].rethrow(f.value)
    ).unsafeToFuture()
  }
}