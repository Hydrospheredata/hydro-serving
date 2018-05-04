package io.hydrosphere.serving.manager.service

import java.io.FileNotFoundException
import java.time.LocalDateTime

import cats.instances.all._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.utils.ops.ModelContractOps._
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.model.{ModelBuildStatus, Result}
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.{Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.repository.ModelBuildRepository
import io.hydrosphere.serving.manager.service.build_script.{BuildScriptManagementService, BuildScriptManagementServiceImpl}
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_build.ModelBuildManagmentServiceImpl
import io.hydrosphere.serving.manager.service.model_build.builders.{ModelBuildService, ModelPushService}
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import org.mockito.{Matchers, Mockito}

import scala.concurrent.Future

class ModelBuildServiceSpec extends GenericUnitTest {
  private[this] val dummyModel = Model(
    id = 1,
    name = "/test_models/tensorflow_model",
    source = "local:test1",
    modelType = ModelType.Unknown("test"),
    description = None,
    modelContract = ModelContract.defaultInstance,
    created = LocalDateTime.now(),
    updated = LocalDateTime.now()
  )

  "ModelBuildService" should "build a correct model without new contract" in {
    val buildRepo = mock[ModelBuildRepository]
    Mockito.when(buildRepo.create(Matchers.any())).thenReturn(
      Future.successful(
        ModelBuild(
          1,
          dummyModel,
          1,
          LocalDateTime.now(),
          None,
          ModelBuildStatus.STARTED,
          None,
          None,
          None
        )
      )
    )

    val scriptS = mock[BuildScriptManagementService]
    Mockito.when(scriptS.fetchScriptForModel(Matchers.any())).thenReturn(
      Future.successful(BuildScriptManagementServiceImpl.defaultBuildScript)
    )

    val versionS = mock[ModelVersionManagementService]
    Mockito.when(versionS.fetchLastModelVersion(1L, None)).thenReturn(
      Result.okF(1L)
    )
    Mockito.when(versionS.create(Matchers.any())).thenReturn(
      Result.okF(
        ModelVersion(
          1,
          "image",
          "tag",
          "sha256",
          LocalDateTime.now(),
          "modelName",
          1,
          ModelType.Unknown("test"),
          Some("source"),
          Some(dummyModel),
          ModelContract.defaultInstance
        )
      )
    )

    val modelS = mock[ModelManagementService]
    Mockito.when(modelS.getModel(1L)).thenReturn(
      Result.okF(dummyModel)
    )

    val pushS = mock[ModelPushService]

    val builder = mock[ModelBuildService]
    Mockito.when(builder.build(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
      Result.okF("kek")
    )

    val service = new ModelBuildManagmentServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder)

    service.buildModel(1, None, None).map { result =>
      assert(result.isRight, result)
      val version = result.right.get
      assert(version.model.get.id === 1L)
    }
  }

  it should "build a correct model with new contract" in {
    val model = dummyModel.copy(id = 1337, name = "tmodel")
    val rawContract = ModelContract("new_contract_test")
    val contract = rawContract.flatten

    val buildRepo = mock[ModelBuildRepository]
    Mockito.when(buildRepo.create(Matchers.any())).thenReturn(
      Future.successful(
        ModelBuild(
          1,
          model.copy(modelContract = rawContract),
          1,
          LocalDateTime.now(),
          None,
          ModelBuildStatus.STARTED,
          None,
          None,
          None
        )
      )
    )

    val scriptS = mock[BuildScriptManagementService]
    Mockito.when(scriptS.fetchScriptForModel(Matchers.any())).thenReturn(
      Future.successful(BuildScriptManagementServiceImpl.defaultBuildScript)
    )

    val versionS = mock[ModelVersionManagementService]
    Mockito.when(versionS.fetchLastModelVersion(1337L, None)).thenReturn(
      Result.okF(1L)
    )
    Mockito.when(versionS.create(Matchers.any())).thenReturn(
      Result.okF(
        ModelVersion(
          1,
          "image",
          "tag",
          "sha256",
          LocalDateTime.now(),
          "modelName",
          1,
          ModelType.Unknown("test"),
          Some("source"),
          Some(model.copy(modelContract = rawContract)),
          rawContract
        )
      )
    )

    val modelS = mock[ModelManagementService]
    Mockito.when(modelS.getModel(1337L)).thenReturn(
      Result.okF(model)
    )
    Mockito.when(modelS.submitFlatContract(Matchers.any(), Matchers.any())).thenReturn(
      Result.okF(model.copy(modelContract = rawContract))
    )

    val pushS = mock[ModelPushService]

    val builder = mock[ModelBuildService]
    Mockito.when(builder.build(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
      Result.okF("kek")
    )

    val service = new ModelBuildManagmentServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder)

    service.buildModel(1337, Some(contract), None).map { result =>
      assert(result.isRight, result)
      val version = result.right.get
      assert(version.model.get.id === 1337L)
      assert(version.model.get.modelContract === rawContract)
      assert(version.modelContract === rawContract)
    }
  }

  it should "not build an incorrect model" in {
    val model = dummyModel.copy(id = 1338, name = "ttmodel")

    val buildRepo = mock[ModelBuildRepository]
    Mockito.when(buildRepo.create(Matchers.any())).thenReturn(
      Future.successful(
        ModelBuild(
          1,
          model,
          1,
          LocalDateTime.now(),
          None,
          ModelBuildStatus.STARTED,
          None,
          None,
          None
        )
      )
    )

    val scriptS = mock[BuildScriptManagementService]
    Mockito.when(scriptS.fetchScriptForModel(Matchers.any())).thenReturn(
      Future.successful(BuildScriptManagementServiceImpl.defaultBuildScript)
    )

    val versionS = mock[ModelVersionManagementService]
    Mockito.when(versionS.fetchLastModelVersion(1338, None)).thenReturn(
      Result.okF(1L)
    )
    Mockito.when(versionS.create(Matchers.any())).thenReturn(
      Result.okF(
        ModelVersion(
          1,
          "image",
          "tag",
          "sha256",
          LocalDateTime.now(),
          "modelName",
          1,
          ModelType.Unknown("test"),
          Some("source"),
          Some(model),
          ModelContract.defaultInstance
        )
      )
    )

    val modelS = mock[ModelManagementService]
    Mockito.when(modelS.getModel(1338)).thenReturn(
      Result.okF(model)
    )
    Mockito.when(modelS.submitFlatContract(Matchers.any(), Matchers.any())).thenReturn(
      Result.okF(model)
    )

    val pushS = mock[ModelPushService]

    val builder = mock[ModelBuildService]
    Mockito.when(builder.build(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
      Result.internalErrorF(new FileNotFoundException("cant find file"))
    )

    val service = new ModelBuildManagmentServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder)

    service.buildModel(1338, None, None).map { result =>
      assert(result.isLeft, result)
      assert(result.left.get.message === "cant find file")
    }
  }
}
