package io.hydrosphere.serving.manager.service

import java.nio.file.Paths
import java.time.LocalDateTime

import cats.data.EitherT
import cats.instances.future._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.model.Result
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.api.ops.ModelContractOps._
import io.hydrosphere.serving.manager.model.db.{Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.repository.ModelBuildRepository
import io.hydrosphere.serving.manager.service.build_script.{BuildScriptManagementService, BuildScriptManagementServiceImpl}
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_build.builders.{ModelBuildService, ModelPushService}
import io.hydrosphere.serving.manager.service.model_build.{BuildModelRequest, ModelBuildManagementServiceImpl}
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus
import org.mockito.{Matchers, Mockito}
import org.scalatest.concurrent.ScalaFutures

import scala.collection.mutable.ListBuffer
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

  describe("Model build service") {
    it("builds a model without contract") {
      eitherTAssert {
        val build = ModelBuild(
          1,
          dummyModel,
          1,
          LocalDateTime.now(),
          None,
          ServiceTaskStatus.Running,
          None,
          None,
          None,
          ""
        )
        val statusHistory = ListBuffer.empty[ModelBuild]

        val buildRepo = mock[ModelBuildRepository]
        when(buildRepo.create(Matchers.any())).thenReturn(
          Future.successful(build)
        )
        when(buildRepo.get(1)).thenReturn(Future.successful(Some(build)))
        when(buildRepo.update(Matchers.any())).thenAnswer { invocation =>
          statusHistory += invocation.getArgumentAt(0, classOf[ModelBuild])
          Future.successful(1)
        }
        when(buildRepo.getRunningBuild(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

        val scriptS = mock[BuildScriptManagementService]
        when(scriptS.fetchScriptForModel(Matchers.any())).thenReturn(
          Future.successful(BuildScriptManagementServiceImpl.defaultBuildScript)
        )

        val versionS = mock[ModelVersionManagementService]
        when(versionS.fetchLastModelVersion(1L, None)).thenReturn(
          Result.okF(1L)
        )
        when(versionS.create(Matchers.any())).thenReturn(
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
        when(modelS.getModel(1L)).thenReturn(
          Result.okF(dummyModel)
        )


        val builder = mock[ModelBuildService]
        when(builder.build(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
          Result.okF("kek")
        )

        val service = new ModelBuildManagementServiceImpl(buildRepo, scriptS, versionS, modelS, null, builder)

        for {
          build <- EitherT(service.buildModel(BuildModelRequest(1)))
          version <- EitherT.liftF(build.future)
          task <- EitherT.liftF(build.taskStatus)
        } yield {
          assert(version.modelName === "modelName")
          assert(version.modelVersion === 1)
          assert(version.model.contains(dummyModel))
        }
      }
    }

    it("should handle a successful build") {
      val model = dummyModel.copy(id = 1337, name = "tmodel")
      val rawContract = ModelContract("new_contract_test")
      val contract = rawContract.flatten
      val build = ModelBuild(
        1,
        model.copy(modelContract = rawContract),
        1,
        LocalDateTime.now(),
        None,
        ServiceTaskStatus.Running,
        None,
        None,
        None,
        ""
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
      Mockito.when(builder.build(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
        Result.okF("sha256")
      )

      val service = new ModelBuildManagementServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder)


      service.buildTaskExecutor.execute(null).future.map { modelVersion =>
        println(modelVersion)
        assert(modelVersion.modelName === build.model.name)
      }
    }

    it("should handle a failed build") {
      val model = dummyModel.copy(id = 1337, name = "tmodel")
      val rawContract = ModelContract("new_contract_test")
      val contract = rawContract.flatten
      val build = ModelBuild(
        1,
        model.copy(modelContract = rawContract),
        1,
        LocalDateTime.now(),
        None,
        ServiceTaskStatus.Running,
        None,
        None,
        None,
        ""
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
      Mockito.when(builder.build(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
        Result.internalErrorF(new RuntimeException("Something bad happened"))
      )

      val service = new ModelBuildManagementServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder)

      val f = service.buildTaskExecutor.execute(null)
      ScalaFutures.whenReady(f.future.failed) { ex =>
        assert(ex.getMessage === "Something bad happened")
      }
    }

    it("should upload and release a model") {
      eitherTAssert {
        val rawContract = ModelContract("new_contract_test")
        val model = dummyModel.copy(id = 1337, name = "tmodel", modelContract = rawContract)
        val upload = ModelUpload(
          Paths.get("."),
          Some("test"),
          Some("unknown:unknown"),
          Some(rawContract),
          None
        )

        val build = ModelBuild(
          1,
          model,
          1,
          LocalDateTime.now(),
          None,
          ServiceTaskStatus.Running,
          None,
          None,
          None,
          ""
        )
        val statusHistory = ListBuffer.empty[ModelBuild]

        val buildRepo = mock[ModelBuildRepository]
        when(buildRepo.create(Matchers.any())).thenReturn(Future.successful(build))
        when(buildRepo.getRunningBuild(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
        when(buildRepo.get(1)).thenReturn(Future.successful(Some(build)))
        when(buildRepo.update(Matchers.any())).thenAnswer { invocation =>
          statusHistory += invocation.getArgumentAt(0, classOf[ModelBuild])
          Future.successful(1)
        }

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
        Mockito.when(modelS.uploadModel(Matchers.any())).thenReturn(Result.okF(model))

        val pushS = mock[ModelPushService]

        val builder = mock[ModelBuildService]
        Mockito.when(builder.build(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
          Result.okF("kek")
        )

        val service = new ModelBuildManagementServiceImpl(buildRepo, scriptS, versionS, modelS, pushS, builder)

        for {
          build <- EitherT(service.uploadAndBuild(upload))
          version <- EitherT.liftF(build.future)
        } yield {
          assert(version.modelName === "test")
          assert(version.modelVersion === 1)
        }
      }
    }
  }
}