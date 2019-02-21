package io.hydrosphere.serving.manager.domain

import java.nio.file.{Path, Paths}
import java.time.LocalDateTime

import cats.Id
import cats.effect.concurrent.Deferred
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.data_profile_types.DataProfileType
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorRepository
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.{Model, ModelRepository, ModelService, ModelVersionMetadata}
import io.hydrosphere.serving.manager.domain.model_build.{BuildResult, ModelVersionBuilder}
import io.hydrosphere.serving.manager.domain.model_version._
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.{FetcherResult, ModelFetcher}
import io.hydrosphere.serving.manager.infrastructure.storage.{ModelFileStructure, ModelUnpacker}
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType
import org.mockito.Matchers

class ModelServiceSpec extends GenericUnitTest {
  describe("Model service") {
    describe("uploads") {
      it("a new model") {
        val model = Model(
          id = 1,
          name = "tf-model"
        )
        val modelName = "tf-model"
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
          profileTypes = Map.empty,
          installCommand = None,
          metadata = Map.empty
        )

        val uploadFile = Paths.get("123123")
        val upload = ModelUploadMetadata(
          name = modelName,
          runtime = modelRuntime,
          hostSelectorName = None,
          contract = Some(contract),
          profileTypes = None,
          installCommand = None
        )

        val modelRepo = mock[ModelRepository[Id]]
        when(modelRepo.get(Matchers.anyLong())).thenReturn(None)

        val storageMock = mock[ModelUnpacker[Id]]
        when(storageMock.unpack(uploadFile)).thenReturn(ModelFileStructure.forRoot(uploadFile))
        when(modelRepo.get(modelName)).thenReturn(None)
        when(modelRepo.create(Model(0, modelName))).thenReturn(model)

        val versionBuilder = mock[ModelVersionBuilder[Id]]
        when(versionBuilder.build(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
          BuildResult(modelVersion, new Deferred[Id, ModelVersion] {
            override def get: Id[ModelVersion] = modelVersion

            override def complete(a: ModelVersion): Id[Unit] = Unit
          })
        )

        val modelVersionService = mock[ModelVersionService[Id]]
        when(modelVersionService.getNextModelVersion(1)).thenReturn(1L)
        val modelVersionRepository = mock[ModelVersionRepository[Id]]
        val selectorRepo = mock[HostSelectorRepository[Id]]

        val fetcher = new ModelFetcher[Id] {
          override def fetch(path: Path): Id[Option[FetcherResult]] = None
        }

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
          profileTypes = Map.empty,
          installCommand = None,
          metadata = Map.empty
        )
        val modelVersionMetadata = ModelVersionMetadata(
          modelName = modelName,
          contract = contract,
          profileTypes = Map.empty,
          runtime = modelRuntime,
          hostSelector = None,
          installCommand = None,
          metadata = Map.empty
        )
        val upload = ModelUploadMetadata(
          name = modelName,
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

        val storageMock = mock[ModelUnpacker[Id]]
        when(storageMock.unpack(uploadFile)).thenReturn(ModelFileStructure.forRoot(Paths.get(".AAAAAAAAA")))

        val versionService = mock[ModelVersionBuilder[Id]]
        when(versionService.build(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
          BuildResult(modelVersion, new Deferred[Id, ModelVersion] {
            override def get: Id[ModelVersion] = modelVersion

            override def complete(a: ModelVersion): Id[Unit] = Unit
          })
        )
        val fetcher = new ModelFetcher[Id] {
          override def fetch(path: Path): Id[Option[FetcherResult]] = None
        }

        val modelManagementService = ModelService[Id](
          modelRepository = modelRepo,
          modelVersionService = null,
          modelVersionRepository = null,
          storageService = storageMock,
          appRepo = null,
          hostSelectorRepository = null,
          fetcher = fetcher,
          modelVersionBuilder = versionService
        )

        val maybeModel = modelManagementService.uploadModel(uploadFile, upload)
        assert(maybeModel.isRight, maybeModel)
        val rModel = maybeModel.right.get.startedVersion
        assert(rModel.model.name === "upload-model", rModel)
      }
    }

    describe("combine metadata") {
      it("uploaded and no fetched") {
        val fetched = None
        val uploaded = ModelUploadMetadata(
          name = "upload-name",
          runtime = DockerImage("test", "test"),
          hostSelectorName = None,
          contract = None,
          profileTypes = Some(Map("a" -> DataProfileType.IMAGE)),
          installCommand = Some("echo hello"),
          metadata = Some(Map("author" -> "me"))
        )
        val res = ModelVersionMetadata.combineMetadata(fetched, uploaded, None)
        assert(res.modelName === "upload-name")
        assert(res.runtime === DockerImage("test", "test"))
        assert(res.contract === ModelContract.defaultInstance.copy(modelName = "upload-name"))
        assert(res.hostSelector === None)
        assert(res.installCommand === Some("echo hello"))
        assert(res.metadata === Map("author" -> "me"))
        assert(res.profileTypes === Map("a" -> DataProfileType.IMAGE))
      }

      it("uploaded and fetched") {
        val contract = ModelContract("asd", signatures = Seq(ModelSignature(
          "sig", Seq.empty, Seq.empty
        )))
        val fetched = Some(FetcherResult(
          modelName = "uuuu",
          modelContract = contract,
          metadata = Map("f" -> "123", "overriden" -> "false")
        ))
        val uploaded = ModelUploadMetadata(
          name = "upload-name",
          runtime = DockerImage("test", "test"),
          hostSelectorName = None,
          contract = None,
          profileTypes = Some(Map("a" -> DataProfileType.IMAGE)),
          installCommand = Some("echo hello"),
          metadata = Some(Map("author" -> "me", "overriden" -> "true"))
        )
        val res = ModelVersionMetadata.combineMetadata(fetched, uploaded, None)
        assert(res.modelName === "upload-name")
        assert(res.runtime === DockerImage("test", "test"))
        assert(res.contract === contract.copy(modelName = "upload-name"))
        assert(res.hostSelector === None)
        assert(res.installCommand === Some("echo hello"))
        assert(res.metadata === Map("author" -> "me", "overriden" -> "true", "f" -> "123"))
        assert(res.profileTypes === Map("a" -> DataProfileType.IMAGE))
      }
    }
  }
}