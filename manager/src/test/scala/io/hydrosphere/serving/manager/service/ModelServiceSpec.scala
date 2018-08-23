package io.hydrosphere.serving.manager.service

import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.model.api.{ModelType, Result}
import io.hydrosphere.serving.manager.model.db.Model
import io.hydrosphere.serving.manager.repository.ModelRepository
import io.hydrosphere.serving.manager.service.model.{ModelManagementServiceImpl, UpdateModelRequest}
import io.hydrosphere.serving.manager.service.source.{ModelStorageService, StorageUploadResult}
import io.hydrosphere.serving.manager.util.TarGzUtils
import org.mockito.Matchers

import scala.concurrent.Future

class ModelServiceSpec extends GenericUnitTest {
  private[this] val dummyModel = Model(
    id = 1,
    name = "/test_models/tensorflow_model",
    modelType = ModelType.Unknown("test"),
    description = None,
    modelContract = ModelContract.defaultInstance,
    created = LocalDateTime.now(),
    updated = LocalDateTime.now()
  )

  def packModel(str: String): Path = {
    val temptar = Files.createTempFile("test_tf_model", ".tar.gz")
    TarGzUtils.compressFolder(Paths.get(getClass.getResource(str).getPath), temptar)
    temptar
  }
  describe("Model management service") {
    describe("uploads") {
      it("new model") {
        val testSourcePath = Files.createTempDirectory("upload-test").toString
        println("Test source path: " + testSourcePath)
        val upload = ModelUpload(
          Paths.get("123123"),
          Some("tf-model"),
          Some("unknown:unknown"),
          None,
          None
        )
        println(upload)
        val model = Model(
          id = 1,
          name = "tf-model",
          modelType = ModelType.Tensorflow("1.1.0"),
          description = None,
          modelContract = ModelContract.defaultInstance,
          created = LocalDateTime.now(),
          updated = LocalDateTime.now()
        )
        val modelRepo = mock[ModelRepository]
        when(modelRepo.get(Matchers.anyLong())).thenReturn(Future.successful(None))

        val sourceMock = mock[ModelStorageService]
        when(sourceMock.upload(Matchers.any())).thenReturn(
          Result.okF(StorageUploadResult(
            "tf-model",
            ModelType.Tensorflow("1.1.0"),
            None,
            ModelContract("tf-model", Seq(ModelSignature()))
          ))
        )
        when(modelRepo.get("tf-model")).thenReturn(Future.successful(None))
        when(modelRepo.create(Matchers.any())).thenReturn(
          Future.successful(model)
        )

        val modelManagementService = new ModelManagementServiceImpl(modelRepo,  null, sourceMock)

        modelManagementService.uploadModel(upload).map { maybeModel =>
          maybeModel.isRight should equal(true)
          val rModel = maybeModel.right.get
          println(rModel)
          rModel.name should equal("tf-model")
        }
      }
      it("existing model") {
        val upload = ModelUpload(
          packModel("/test_models/tensorflow_model"),
          Some("upload-model"),
          Some("unknown:unknown"),
          Some(ModelContract.defaultInstance),
          None
        )
        println(upload)
        val model = Model(
          id = 1,
          name = "upload-model",
          modelType = ModelType.Tensorflow("1.1.0"),
          description = None,
          modelContract = ModelContract.defaultInstance,
          created = LocalDateTime.now(),
          updated = LocalDateTime.now()
        )

        val modelRepo = mock[ModelRepository]
        when(modelRepo.update(Matchers.any(classOf[Model]))).thenReturn(Future.successful(1))
        when(modelRepo.get("upload-model")).thenReturn(Future.successful(Some(model)))
        when(modelRepo.get(1)).thenReturn(Future.successful(Some(model)))

        val sourceMock = mock[ModelStorageService]
        when(sourceMock.upload(Matchers.any())).thenReturn(
          Result.okF(StorageUploadResult(
            "upload-model",
            ModelType.Unknown(),
            None,
            ModelContract("upload-model", Seq(ModelSignature()))
          ))
        )
        when(sourceMock.rename("upload-model", "upload-model")).thenReturn(Result.okF(Paths.get("some-test-path")))

        val modelManagementService = new ModelManagementServiceImpl(modelRepo, null, sourceMock)

        modelManagementService.uploadModel(upload).map { maybeModel =>
          assert(maybeModel.isRight, maybeModel)
          val rModel = maybeModel.right.get
          assert(rModel.name === "upload-model", rModel)
          assert(rModel.modelType === ModelType.Unknown())
        }
      }
    }

    describe("update") {
      describe("fails") {
        it("when model doesn't exist") {
          val updateRequest = UpdateModelRequest(
            id = 100,
            name = "new_model",
            ModelType.Tensorflow("1.1.0"),
            description = None,
            modelContract = ModelContract.defaultInstance
          )

          val modelRepoMock = mock[ModelRepository]
          when(modelRepoMock.get(100)).thenReturn(Future.successful(None))

          val modelService = new ModelManagementServiceImpl(modelRepoMock, null, null)

          modelService.updateModel(updateRequest).map { result =>
            info(result.toString)
            assert(result.isLeft, result)
          }
        }
        it("when updated model has non-unique name") {

          val updateRequest = UpdateModelRequest(
            id = 1,
            name = "new_model",
            ModelType.Tensorflow("1.1.0"),
            description = Some("I am updated"),
            modelContract = ModelContract.defaultInstance
          )
          val oldModel = Model(
            id = 1,
            name = "old_model",
            modelType = ModelType.Unknown(),
            description = Some("I am old"),
            modelContract = ModelContract.defaultInstance,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
          )
          val conflictModel = Model(
            id = 2,
            name = "new_model",
            modelType = ModelType.Unknown(),
            description = Some("I already have this name"),
            modelContract = ModelContract.defaultInstance,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
          )

          val modelRepoMock = mock[ModelRepository]
          when(modelRepoMock.get(1)).thenReturn(Future.successful(Some(oldModel)))
          when(modelRepoMock.get("new_model")).thenReturn(Future.successful(Some(conflictModel)))

          val modelService = new ModelManagementServiceImpl(modelRepoMock, null, null)

          modelService.updateModel(updateRequest).map { result =>
            info(result.toString)
            assert(result.isLeft, result)
          }
        }
      }
      describe("succeeds") {
        it("when model has unique name") {
          val updateRequest = UpdateModelRequest(
            id = 1,
            name = "new_model",
            ModelType.Tensorflow("1.1.0"),
            description = Some("I am updated"),
            modelContract = ModelContract.defaultInstance
          )
          val oldModel = Model(
            id = 1,
            name = "old_model",
            modelType = ModelType.Unknown(),
            description = Some("I am old"),
            modelContract = ModelContract.defaultInstance,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
          )
          val otherModel = Model(
            id = 2,
            name = "other_model",
            modelType = ModelType.Unknown(),
            description = Some("I am"),
            modelContract = ModelContract.defaultInstance,
            created = LocalDateTime.now(),
            updated = LocalDateTime.now()
          )

          val modelRepoMock = mock[ModelRepository]
          when(modelRepoMock.get(1)).thenReturn(Future.successful(Some(oldModel)))
          when(modelRepoMock.get("new_model")).thenReturn(Future.successful(None))
          when(modelRepoMock.update(Matchers.any())).thenReturn(Future.successful(1))

          val storageMock = mock[ModelStorageService]
          when(storageMock.rename("old_model", "new_model")).thenReturn(Result.okF(Paths.get("some-good-path")))

          val modelService = new ModelManagementServiceImpl(modelRepoMock, null, storageMock)

          modelService.updateModel(updateRequest).map { result =>
            assert(result.isRight, result)
          }
        }
      }
    }
  }
}