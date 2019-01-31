package io.hydrosphere.serving.manager.domain

import java.nio.file.Paths
import java.time.LocalDateTime

import cats.Id
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorRepository
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.{Model, ModelRepository, ModelService, ModelVersionMetadata}
import io.hydrosphere.serving.manager.domain.model_build.ModelVersionBuilder
import io.hydrosphere.serving.manager.domain.model_version._
import io.hydrosphere.serving.manager.infrastructure.storage.ModelStorage
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.ModelFetcher
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType
import org.mockito.Matchers

import scala.concurrent.Future

class ModelServiceSpec extends GenericUnitTest {
  describe("Model service") {
    describe("uploads") {
      it("a new model") {
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
          modelName,
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
          modelContract = contract,
          runtime = modelRuntime,
          model = model,
          hostSelector = None,
          status = ModelVersionStatus.Released,
          profileTypes = Map.empty
        )
        val modelVersionMetadata = ModelVersionMetadata(
          modelName = modelName,
          contract = contract,
          profileTypes = Map.empty,
          runtime = modelRuntime,
          hostSelector = None
        )

        val uploadFile = Paths.get("123123")
        val upload = ModelUploadMetadata(
          name = Some(modelName),
          runtime = modelRuntime,
          hostSelectorName = None,
          contract = Some(contract),
          profileTypes = None
        )

        val modelRepo = mock[ModelRepository[Id]]
        when(modelRepo.get(Matchers.anyLong())).thenReturn(None)

        val storageMock = mock[ModelStorage[Id]]
        when(storageMock.unpack(uploadFile, upload.name)).thenReturn(Paths.get(".kekpath"))
        when(modelRepo.get(modelName)).thenReturn(None)
        when(modelRepo.create(Model(0, modelName))).thenReturn(model)

        val versionBuilder = mock[ModelVersionBuilder[Id]]
        when(versionBuilder.build(Matchers.any(), Matchers.any())).thenReturn(
          BuildResult(modelVersion, Future.successful(modelVersion))
        )

        val modelVersionService = mock[ModelVersionService[Id]]
        when(modelVersionService.getNextModelVersion(1)).thenReturn(1L)
        val modelVersionRepository = mock[ModelVersionRepository[Id]]
        val selectorRepo = mock[HostSelectorRepository[Id]]

        val fetcher = mock[ModelFetcher[Id]]

        val modelManagementService = ModelService[Id](
          modelRepository = modelRepo,
          modelVersionService = modelVersionService,
          modelVersionRepository = modelVersionRepository,
          storageService = storageMock,
          appRepo = null,
          hostSelectorRepository = selectorRepo,
          fetcher = fetcher,
          modelVersionBuilder = versionBuilder
        )

        val maybeModel = modelManagementService.uploadModel(uploadFile, upload)
        assert(maybeModel.isRight, maybeModel)
        val rModel = maybeModel.right.get.startedVersion
        println(rModel)
        assert(rModel.model.name === "tf-model")
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
          modelName,
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
          modelContract = contract,
          runtime = modelRuntime,
          model = model,
          hostSelector = None,
          status = ModelVersionStatus.Released,
          profileTypes = Map.empty
        )
        val modelVersionMetadata = ModelVersionMetadata(
          modelName = modelName,
          contract = contract,
          profileTypes = Map.empty,
          runtime = modelRuntime,
          hostSelector = None
        )
        val upload = ModelUploadMetadata(
          name = Some(modelName),
          runtime = modelRuntime,
          hostSelectorName = None,
          contract = Some(contract),
          profileTypes = None
        )
        println(upload)

        val modelRepo = mock[ModelRepository[Id]]
        when(modelRepo.update(Matchers.any(classOf[Model]))).thenReturn(1)
        when(modelRepo.get(modelName)).thenReturn(Some(model))
        when(modelRepo.get(1)).thenReturn(Some(model))

        val storageMock = mock[ModelStorage[Id]]
        when(storageMock.unpack(uploadFile, upload.name)).thenReturn(Paths.get(".AAAAAAAAA"))
        when(storageMock.rename(modelName, modelName)).thenReturn(Some(Paths.get("some-test-path")))

        val versionService = mock[ModelVersionBuilder[Id]]
        when(versionService.build(model, modelVersionMetadata)).thenReturn(BuildResult(modelVersion, Future.successful(modelVersion)))

        val modelManagementService = ModelService[Id](
          modelRepository = modelRepo,
          modelVersionService = null,
          modelVersionRepository = null,
          storageService = storageMock,
          appRepo = null,
          hostSelectorRepository = null,
          fetcher = null,
          modelVersionBuilder = versionService
        )

        val maybeModel = modelManagementService.uploadModel(uploadFile, upload)
        assert(maybeModel.isRight, maybeModel)
        val rModel = maybeModel.right.get.startedVersion
        assert(rModel.model.name === "upload-model", rModel)
      }
    }
  }
}