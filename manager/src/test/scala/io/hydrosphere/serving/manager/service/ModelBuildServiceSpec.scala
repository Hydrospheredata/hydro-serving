package io.hydrosphere.serving.manager.service

import java.io.FileNotFoundException
import java.nio.file.Paths
import java.time.LocalDateTime

import cats.instances.all._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.utils.ops.ModelContractOps._
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.controller.application.{ExecutionGraphRequest, ExecutionStepRequest, SimpleServiceDescription}
import io.hydrosphere.serving.manager.controller.model.{ModelDeploy, ModelUpload}
import io.hydrosphere.serving.manager.model.{ModelBuildStatus, Result}
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.repository.ModelBuildRepository
import io.hydrosphere.serving.manager.service.application.{ApplicationManagementService, ApplicationManagementServiceImpl}
import io.hydrosphere.serving.manager.service.build_script.{BuildScriptManagementService, BuildScriptManagementServiceImpl}
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_build.ModelBuildManagementServiceImpl
import io.hydrosphere.serving.manager.service.model_build.builders.{ModelBuildService, ModelPushService}
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.manager.service.runtime.{RuntimeManagementService, RuntimeManagementServiceImpl}
import org.mockito.{Matchers, Mockito}

import scala.concurrent.Future

class ModelBuildServiceSpec extends GenericUnitTest {
  private[this] val dummyModel = Model(
    id = 1,
    name = "/test_models/tensorflow_model",
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
    Mockito.when(buildRepo.getRunningBuild(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

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

    val service = new ModelBuildManagementServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder, null, null)

    service.buildAndOverrideContract(1, None, None).map { result =>
      assert(result.isRight, result)
      val modelBuild = result.right.get
      assert(modelBuild.model.id === 1L)
    }
  }

  it should "start a build" in {
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
    Mockito.when(buildRepo.getRunningBuild(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

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

    val service = new ModelBuildManagementServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder, null, null)

    service.buildAndOverrideContract(1337, Some(contract), None).map { result =>
      assert(result.isRight, result)
      val modelBuild = result.right.get
      assert(modelBuild.model.id === 1337L)
      assert(modelBuild.model.modelContract === rawContract)
    }
  }

  it should "handle a successful build" in {
    val model = dummyModel.copy(id = 1337, name = "tmodel")
    val rawContract = ModelContract("new_contract_test")
    val contract = rawContract.flatten
    val build = ModelBuild(
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

    val buildRepo = mock[ModelBuildRepository]
    Mockito.when(buildRepo.create(Matchers.any())).thenReturn(Future.successful(build))
    Mockito.when(buildRepo.getRunningBuild(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

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
          "tmodel",
          1,
          ModelType.Unknown("test"),
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
      Result.okF("sha256")
    )

    val service = new ModelBuildManagementServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder)

    service.handleBuild(build, "test").map { result =>
      assert(result.isRight, result)
      val modelVersion = result.right.get
      println(modelVersion)
      assert(modelVersion.modelName === build.model.name)
    }
  }

  it should "handle a failed build" in {
    val model = dummyModel.copy(id = 1337, name = "tmodel")
    val rawContract = ModelContract("new_contract_test")
    val contract = rawContract.flatten
    val build = ModelBuild(
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

    val buildRepo = mock[ModelBuildRepository]
    Mockito.when(buildRepo.create(Matchers.any())).thenReturn(Future.successful(build))
    Mockito.when(buildRepo.getRunningBuild(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

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
          "tmodel",
          1,
          ModelType.Unknown("test"),
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
      Result.internalErrorF(new RuntimeException("Something bad happened"))
    )

    val service = new ModelBuildManagmentServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder, null, null)
    val service = new ModelBuildManagementServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder)

    service.handleBuild(build, "test").map { result =>
      assert(result.isLeft, result)
    }
  }

  it should "upload and release a model" in {
    val file = Paths.get(".")
    val rawContract = ModelContract("new_contract_test")
    val model = dummyModel.copy(id = 1337, name = "tmodel", modelContract = rawContract)
    val upload = ModelUpload(
      Some("test"),
      Some("unknown:unknown"),
      Some(rawContract),
      None
    )
    val contract = rawContract.flatten

    val build = ModelBuild(
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
    val buildRepo = mock[ModelBuildRepository]
    Mockito.when(buildRepo.create(Matchers.any())).thenReturn(Future.successful(build))
    Mockito.when(buildRepo.getRunningBuild(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

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
    Mockito.when(modelS.uploadModel(Matchers.any(), Matchers.any())).thenReturn(Result.okF(model))

    val pushS = mock[ModelPushService]

    val builder = mock[ModelBuildService]
    Mockito.when(builder.build(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
      Result.okF("kek")
    )

    val service = new ModelBuildManagementServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder, null, null)

    service.uploadAndBuild(file, upload).map { result =>
      assert(result.isRight, result)
      val modelBuild = result.right.get
      assert(modelBuild.model.id === 1337L)
      assert(modelBuild.model.modelContract === rawContract)
      assert(modelBuild.model.modelContract === rawContract)
    }
  }

  it should "deploy new model" in {
    val file = Paths.get(".")
    val upload = ModelUpload(
      Some("test"),
      Some("unknown:unknown"),
      Some(ModelContract.defaultInstance),
      Some("system")
    )

    val deploy = ModelDeploy(
      upload,
      "dummy",
      "latest"
    )

    val model = dummyModel.copy(id = 1337, name = "test", namespace = Some("system"))
    val rawContract = ModelContract("new_contract_test")
    val contract = rawContract.flatten

    val build = ModelBuild(
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
    val buildRepo = mock[ModelBuildRepository]
    Mockito.when(buildRepo.create(Matchers.any())).thenReturn(Future.successful(build))
    Mockito.when(buildRepo.getRunningBuild(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

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
          "test",
          1,
          ModelType.Unknown("test"),
          Some(model.copy(modelContract = rawContract)),
          rawContract
        )
      )
    )

    val modelS = mock[ModelManagementService]
    Mockito.when(modelS.getModel(1337L)).thenReturn(
      Result.okF(model)
    )
    Mockito.when(modelS.uploadModel(Matchers.any(), Matchers.any())).thenReturn(Result.okF(model))

    val pushS = mock[ModelPushService]

    val builder = mock[ModelBuildService]
    Mockito.when(builder.build(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
      Result.okF("kek")
    )

    val runtimeS = mock[RuntimeManagementService]
    Mockito.when(runtimeS.get("dummy", "latest")).thenReturn(
      Result.okF(Runtime(1, "dummy", "latest", List.empty, List.empty, Map.empty))
    )

    val appS = mock[ApplicationManagementService]
    val expectedGraph = ExecutionGraphRequest(
      Seq(ExecutionStepRequest(
        List(SimpleServiceDescription(
          1,
          Some(1),
          None,
          100,
          "default"
        ))
      ))
    )
    Mockito.when(appS.createApplication("test:1", Some("system"), expectedGraph, Seq.empty)).thenReturn(
      Result.okF(Application(1, "test:1", Some("system"), ModelContract.defaultInstance, ApplicationExecutionGraph(List.empty), List.empty))
    )

    val service = new ModelBuildManagmentServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder, runtimeS, appS)

    service.uploadAndDeploy(file, deploy).map { result =>
      assert(result.isRight, result)
      val application = result.right.get
      assert(application.id === 1)
      assert(application.namespace.contains("system"), application.namespace)
    }
  }
}
