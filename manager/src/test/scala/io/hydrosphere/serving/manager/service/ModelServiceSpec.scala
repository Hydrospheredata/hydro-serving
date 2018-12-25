package io.hydrosphere.serving.manager.service

import java.nio.file.{Files, Path, Paths}
import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.domain.host_selector.{AnyHostSelector, HostSelectorServiceAlg}
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.{Model, ModelRepositoryAlgebra, ModelService, ModelVersionMetadata}
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionServiceAlg, ModelVersionStatus, ModelVersionView}
import io.hydrosphere.serving.manager.infrastructure.storage.ModelStorageService
import io.hydrosphere.serving.manager.util.TarGzUtils
import io.hydrosphere.serving.model.api.{HFResult, ModelMetadata, ModelType, Result}
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType
import org.mockito.Matchers

import scala.concurrent.Future

class ModelServiceSpec extends GenericUnitTest {
  def packModel(str: String): Path = {
    val temptar = Files.createTempFile("test_tf_model", ".tar.gz")
    TarGzUtils.compressFolder(Paths.get(getClass.getResource(str).getPath), temptar)
    temptar
  }
  describe("Model management service") {
    describe("uploads") {
      it("a new model") {
        val testSourcePath = Files.createTempDirectory("upload-test").toString
        println("Test source path: " + testSourcePath)
        val model = Model(
          id = 1,
          name = "tf-model"
        )
        val modelName = "tf-model"
        val modelType = ModelType.fromTag("type:1")
        val modelRuntime = DockerImage(
          name = "runtime",
          tag = "latest"
        )
        val contract = ModelContract(
          "test",
          Seq(ModelSignature(
            "testSig",
            Seq(ModelField("in", TensorShape.scalar.toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE))),
            Seq(ModelField("out", TensorShape.scalar.toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE)))
          ))
        )
        val modelVersion = ModelVersion(
          id = 1,
          image = DockerImage(
            name = "tf-model",
            tag = "1"
          ),
          created = LocalDateTime.now(),
          finished = Some(LocalDateTime.now()),
          modelVersion = 1,
          modelType = modelType,
          modelContract = contract,
          runtime = modelRuntime,
          model = model,
          hostSelector = None,
          status = ModelVersionStatus.Finished,
          profileTypes = Map.empty
        )
        val modelVersionMetadata = ModelVersionMetadata(
          modelName = modelName,
          modelType = modelType,
          contract = contract,
          profileTypes = Map.empty,
          runtime = modelRuntime,
          hostSelector = AnyHostSelector
        )

        val uploadFile = Paths.get("123123")
        val upload = ModelUploadMetadata(
          name = Some(modelName),
          runtime = modelRuntime,
          modelType = Some(modelType),
          hostSelectorName = None,
          contract = Some(contract),
          profileTypes = None
        )
        val modelRepo = mock[ModelRepositoryAlgebra[Future]]
        when(modelRepo.get(Matchers.anyLong())).thenReturn(Future.successful(None))

        val sourceMock = mock[ModelStorageService]
        when(sourceMock.unpack(uploadFile, upload.name)).thenReturn(
          Result.okF(ModelMetadata(
            modelName,
            ModelType.Tensorflow("1.1.0"),  // will be overridden
            ModelContract(  // will be overridden
              "test",
              Seq(ModelSignature(
                "testSig",
                Seq(ModelField("in", TensorShape.scalar.toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE))),
                Seq(ModelField("out", TensorShape.scalar.toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE)))
              ))
            )
          ))
        )
        when(modelRepo.get(modelName)).thenReturn(Future.successful(None))
        when(modelRepo.create(Model(0, modelName))).thenReturn(
          Future.successful(model)
        )

        val versionService = mock[ModelVersionServiceAlg]
        when(versionService.build(model, modelVersionMetadata)).thenReturn(
          Result.okF(modelVersion)
        )

        val modelManagementService = new ModelService(modelRepo, versionService, sourceMock, null, null)

        modelManagementService.uploadModel(uploadFile, upload).map { maybeModel =>
          assert(maybeModel.isRight, maybeModel)
          val rModel = maybeModel.right.get
          println(rModel)
          assert(rModel.model.name === "tf-model")
        }
      }

      it("existing model") {
        val uploadFile = Paths.get("123123")
        val modelName = "upload-model"
        val modelType = ModelType.Unknown()
        val modelRuntime = DockerImage(
          name = "runtime",
          tag = "latest"
        )
        val model = Model(
          id = 1,
          name = modelName,
        )
        val contract = ModelContract(
          "test",
          Seq(ModelSignature(
            "testSig",
            Seq(ModelField("in", TensorShape.scalar.toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE))),
            Seq(ModelField("out", TensorShape.scalar.toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE)))
          ))
        )
        val modelVersion = ModelVersion(
          id = 1,
          image = DockerImage(
            name = modelName,
            tag = "1"
          ),
          created = LocalDateTime.now(),
          finished = Some(LocalDateTime.now()),
          modelVersion = 1,
          modelType = modelType,
          modelContract = contract,
          runtime = modelRuntime,
          model = model,
          hostSelector = None,
          status = ModelVersionStatus.Finished,
          profileTypes = Map.empty
        )
        val modelVersionMetadata = ModelVersionMetadata(
          modelName = modelName,
          modelType = modelType,
          contract = contract,
          profileTypes = Map.empty,
          runtime = modelRuntime,
          hostSelector = AnyHostSelector
        )
        val upload = ModelUploadMetadata(
          name = Some(modelName),
          runtime = modelRuntime,
          modelType = Some(modelType),
          hostSelectorName = None,
          contract = Some(contract),
          profileTypes = None
        )
        println(upload)

        val modelRepo = mock[ModelRepositoryAlgebra[Future]]
        when(modelRepo.update(Matchers.any(classOf[Model]))).thenReturn(Future.successful(1))
        when(modelRepo.get(modelName)).thenReturn(Future.successful(Some(model)))
        when(modelRepo.get(1)).thenReturn(Future.successful(Some(model)))

        val sourceMock = mock[ModelStorageService]
        when(sourceMock.unpack(uploadFile, upload.name)).thenReturn(
          Result.okF(ModelMetadata(
            modelName,
            ModelType.Unknown(),
            ModelContract(
              "test",
              Seq(ModelSignature(
                "testSig",
                Seq(ModelField("in", TensorShape.scalar.toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE))),
                Seq(ModelField("out", TensorShape.scalar.toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE)))
              ))
            )
          ))
        )
        when(sourceMock.rename(modelName, modelName)).thenReturn(Result.okF(Paths.get("some-test-path")))

        val versionService = mock[ModelVersionServiceAlg]
        when(versionService.build(model, modelVersionMetadata)).thenReturn(
          Result.okF(modelVersion)
        )

        val hService = mock[HostSelectorServiceAlg]

        val modelManagementService = new ModelService(modelRepo, versionService, sourceMock, null, hService)

        modelManagementService.uploadModel(uploadFile, upload).map { maybeModel =>
          assert(maybeModel.isRight, maybeModel)
          val rModel = maybeModel.right.get
          assert(rModel.model.name === "upload-model", rModel)
          assert(rModel.modelType === ModelType.Unknown())
        }
      }
    }
  }
}