package io.hydrosphere.serving.manager.api.grpc

import cats.data.EitherT
import cats.effect.Effect
import cats.syntax.applicativeError._
import cats.syntax.functor._
import com.google.protobuf.empty.Empty
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.domain
import io.hydrosphere.serving.manager.api.GetVersionRequest
import io.hydrosphere.serving.manager.api.ManagerServiceGrpc.ManagerService
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersionRepository, ModelVersion => DMV}
import io.hydrosphere.serving.manager.grpc

import scala.concurrent.Future

class ManagerGrpcService[F[_]: Effect](versionRepositoryAlgebra: ModelVersionRepository[F]) extends ManagerService {
  
  private def modelVersionToGrpcEntity(mv: domain.model_version.ModelVersion): grpc.entities.ModelVersion = grpc.entities.ModelVersion(
    id = mv.id,
    version = mv.modelVersion,
    modelType = "",
    status = mv.status.toString,
    selector = mv.hostSelector.map(s => grpc.entities.HostSelector(s.id, s.name)),
    model = Some(grpc.entities.Model(mv.model.id, mv.model.name)),
    contract = Some(ModelContract(mv.modelContract.modelName, mv.modelContract.predict)),
    image = Some(grpc.entities.DockerImage(mv.image.name, mv.image.tag)),
    imageSha = mv.image.sha256.getOrElse(""),
    runtime = Some(grpc.entities.DockerImage(mv.runtime.name, mv.runtime.tag))
  )
  
  override def getAllVersions(request: Empty, responseObserver: StreamObserver[grpc.entities.ModelVersion]): Unit = {
    val fAction = versionRepositoryAlgebra.all().map { versions =>
      versions.foreach { v =>
        responseObserver.onNext(modelVersionToGrpcEntity(v))
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

  override def getVersion(request: GetVersionRequest): Future[grpc.entities.ModelVersion] = {
    val f = for {
      version <- EitherT.fromOptionF[F, Throwable, DMV](versionRepositoryAlgebra.get(request.id), new IllegalArgumentException(s"ModelVersion with id ${request.id} is not found"))
    } yield modelVersionToGrpcEntity(version)

    Effect[F].toIO(
      Effect[F].rethrow(f.value)
    ).unsafeToFuture()
  }
}