package io.hydrosphere.serving.manager.api

import java.time.LocalDateTime

import com.google.protobuf.empty.Empty
import io.grpc.stub.StreamObserver
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.api.grpc.ManagerGrpcService
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersionRepositoryAlgebra, ModelVersionStatus, ModelVersion => DMV}
import io.hydrosphere.serving.manager.grpc.entities.ModelVersion
import io.hydrosphere.serving.model.api.ModelType

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

class GrpcSpec extends GenericUnitTest {
  describe("Manager GRPC API") {
    it("should return ModelVersion for id") {
      val versionRepo = mock[ModelVersionRepositoryAlgebra[Future]]
      when(versionRepo.get(1)).thenReturn(Future.successful(Option(
        DMV(
          id = 1,
          image = DockerImage("test", "test"),
          created = LocalDateTime.now(),
          finished = None,
          modelVersion = 1,
          modelType = ModelType.Unknown(),
          modelContract = ModelContract.defaultInstance,
          runtime = DockerImage("asd", "asd"),
          model = Model(1, "asd"),
          hostSelector = None,
          status = ModelVersionStatus.Started,
          profileTypes = Map.empty
        )
      )))
      when(versionRepo.get(1000)).thenReturn(Future.successful(None))
      val grpcApi = new ManagerGrpcService(versionRepo)

      grpcApi.getVersion(GetVersionRequest(1000)).onComplete {
        case Success(_) => fail("Value instead of exception")
        case Failure(exception) => assert(exception.isInstanceOf[IllegalArgumentException])
      }

      grpcApi.getVersion(GetVersionRequest(1)).map { mv =>
        assert(mv.id === 1)
      }
    }
    it("should return a stream of all ModelVersions") {
      val versionRepo = mock[ModelVersionRepositoryAlgebra[Future]]
      when(versionRepo.all()).thenReturn(Future.successful(Seq(
        DMV(
          id = 1,
          image = DockerImage("test", "test"),
          created = LocalDateTime.now(),
          finished = None,
          modelVersion = 1,
          modelType = ModelType.Unknown(),
          modelContract = ModelContract.defaultInstance,
          runtime = DockerImage("asd", "asd"),
          model = Model(1, "asd"),
          hostSelector = None,
          status = ModelVersionStatus.Started,
          profileTypes = Map.empty
        ),
        DMV(
          id = 2,
          image = DockerImage("test", "test"),
          created = LocalDateTime.now(),
          finished = None,
          modelVersion = 1,
          modelType = ModelType.Unknown(),
          modelContract = ModelContract.defaultInstance,
          runtime = DockerImage("asd", "asd"),
          model = Model(1, "asd"),
          hostSelector = None,
          status = ModelVersionStatus.Started,
          profileTypes = Map.empty
        )
      )))

      val buffer = ListBuffer.empty[ModelVersion]
      var completionFlag = false
      val observer = new StreamObserver[ModelVersion] {
        override def onNext(value: ModelVersion): Unit = buffer += value

        override def onError(t: Throwable): Unit = ???

        override def onCompleted(): Unit = completionFlag = true
      }

      val grpcApi = new ManagerGrpcService(versionRepo)
      grpcApi.getAllVersions(Empty(), observer)

      Future {
        assert(buffer.map(_.id) === Seq(1, 2))
        assert(completionFlag)
      }
    }
    it("should handle ModelVersion stream error") {
      val versionRepo = mock[ModelVersionRepositoryAlgebra[Future]]
      when(versionRepo.all()).thenReturn(Future.failed(new IllegalStateException("AAAAAAA")))

      val errors = ListBuffer.empty[Throwable]
      val observer = new StreamObserver[ModelVersion] {
        override def onNext(value: ModelVersion): Unit = ???

        override def onError(t: Throwable): Unit = errors += t

        override def onCompleted(): Unit = ???
      }

      val grpcApi = new ManagerGrpcService(versionRepo)
      grpcApi.getAllVersions(Empty(), observer)
      Future {
        assert(errors.nonEmpty)
      }
    }
  }
}