package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.model.api.{ModelType, Result}
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.service.aggregated_info.AggregatedInfoUtilityServiceImpl
import io.hydrosphere.serving.manager.service.application.ApplicationManagementService
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_build.ModelBuildManagmentService
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus
import org.mockito.{Matchers, Mockito}

import scala.concurrent.Future

class AggregatedInfoServiceSpec extends GenericUnitTest {

  describe("Aggregated info service") {
    it("should return info for ready to build models") {
      val createdTime = LocalDateTime.now()

      val models = Seq(
        Model(1, "model1", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime),
        Model(2, "model2", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime)
      )

      val modelMock = mock[ModelManagementService]
      Mockito.when(modelMock.allModels()).thenReturn(
        Future.successful(models)
      )

      val buildMock = mock[ModelBuildManagmentService]
      Mockito.when(buildMock.lastForModels(Matchers.any())).thenReturn(
        Future.successful(Seq.empty)
      )
      val versionMock = mock[ModelVersionManagementService]
      Mockito.when(versionMock.lastModelVersionForModels(Matchers.any())).thenReturn(
        Future.successful(Seq.empty)
      )
      val appMock = mock[ApplicationManagementService]
      Mockito.when(appMock.allApplications()).thenReturn(
        Future.successful(Seq.empty)
      )

      val aggService = new AggregatedInfoUtilityServiceImpl(modelMock, buildMock, versionMock, appMock)

      aggService.allModelsAggregatedInfo().map { result =>
        assert(result.nonEmpty)
        assert(result.forall(_.lastModelBuild.isEmpty), result)
        assert(result.forall(_.lastModelVersion.isEmpty), result)
        assert(result.forall(_.nextVersion.isDefined))
        val iModels = result.map(_.model)
        assert(iModels.exists(_.name == "model1"))
        assert(iModels.exists(_.name == "model2"))
      }
    }

    it("should return info for built models") {
      val createdTime = LocalDateTime.now()

      val unbuiltModel = Model(2, "model2", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime)
      val builtModel = Model(1, "model1", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime)
      val mVersion = ModelVersion(1, "image", "tag", "sha256", createdTime, builtModel.name, 1, ModelType.Tensorflow("1.1.0"), Some(builtModel), builtModel.modelContract)
      val mBuild = ModelBuild(1, builtModel, 1, createdTime, Some(LocalDateTime.now()), ServiceTaskStatus.Finished, None, None, Some(mVersion), "")

      val models = Seq(builtModel, unbuiltModel)

      val modelMock = mock[ModelManagementService]
      Mockito.when(modelMock.allModels()).thenReturn(
        Future.successful(models)
      )

      val buildMock = mock[ModelBuildManagmentService]
      Mockito.when(buildMock.lastForModels(Matchers.any())).thenReturn(
        Future.successful(Seq(mBuild))
      )
      val versionMock = mock[ModelVersionManagementService]
      Mockito.when(versionMock.lastModelVersionForModels(Matchers.any())).thenReturn(
        Future.successful(Seq(mVersion))
      )
      val appMock = mock[ApplicationManagementService]
      Mockito.when(appMock.allApplications()).thenReturn(
        Future.successful(Seq.empty)
      )

      val aggService = new AggregatedInfoUtilityServiceImpl(modelMock, buildMock, versionMock, appMock)

      aggService.allModelsAggregatedInfo().map { result =>
        assert(result.nonEmpty)
        val m1 = result.find(_.model.name == "model1").get
        val m2 = result.find(_.model.name == "model2").get

        assert(m1.model === builtModel)
        assert(m1.lastModelBuild.get === mBuild)
        assert(m1.lastModelVersion.get === mVersion)
        assert(m1.nextVersion.isEmpty)

        assert(m2.model === unbuiltModel)
        assert(m2.lastModelVersion.isEmpty)
        assert(m2.lastModelBuild.isEmpty)
        assert(m2.nextVersion.get === 1)
      }
    }

    describe("delete") {
      it("fails when model has at least one version in application") {
        val model = Model(1, "model", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, LocalDateTime.now(), LocalDateTime.now())
        val version = ModelVersion(1, "image", "tag", "sha256", LocalDateTime.now(), "model", 1, ModelType.Tensorflow("1.1.0"), Some(model), ModelContract.defaultInstance)
        val app = Application(1, "app", None, ModelContract.defaultInstance, ApplicationExecutionGraph(List.empty), List.empty)

        val modelMock = mock[ModelManagementService]
        when(modelMock.getModel(1)).thenReturn(Result.okF(model))

        val versionMock = mock[ModelVersionManagementService]
        when(versionMock.listForModel(1)).thenReturn(Result.okF(Seq(version)))

        val appMock = mock[ApplicationManagementService]
        when(appMock.findVersionUsage(1)).thenReturn(Future.successful(Seq(app)))

        val service = new AggregatedInfoUtilityServiceImpl(modelMock, null, versionMock, appMock)

        service.deleteModel(1).map{ result =>
          info(result.toString)
          assert(result.isLeft, result)
          val err = result.left.get.message
          assert(err.contains("Can't delete the model"))
          assert(err.contains("app"))
        }
      }

      it("succeeds when model doesn't have versions in applications") {
        val model = Model(1, "model", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, LocalDateTime.now(), LocalDateTime.now())
        val version = ModelVersion(1, "image", "tag", "sha256", LocalDateTime.now(), "model", 1, ModelType.Tensorflow("1.1.0"), Some(model), ModelContract.defaultInstance)
        val build = ModelBuild(1, model, 1, LocalDateTime.now(), Some(LocalDateTime.now()), ServiceTaskStatus.Finished, None, None, Some(version), "")

        val modelMock = mock[ModelManagementService]
        when(modelMock.getModel(1)).thenReturn(Result.okF(model))
        when(modelMock.delete(1)).thenReturn(Result.okF(model))

        val buildMock = mock[ModelBuildManagmentService]
        when(buildMock.listForModel(1)).thenReturn(Result.okF(Seq(build)))
        when(buildMock.delete(1)).thenReturn(Result.okF(build))

        val versionMock = mock[ModelVersionManagementService]
        when(versionMock.listForModel(1)).thenReturn(Result.okF(Seq(version)))
        when(versionMock.delete(1)).thenReturn(Result.okF(version))

        val appMock = mock[ApplicationManagementService]
        when(appMock.findVersionUsage(1)).thenReturn(Future.successful(Seq.empty))

        val service1 = new AggregatedInfoUtilityServiceImpl(modelMock, buildMock, versionMock, appMock)

        service1.deleteModel(1).map{ result =>
          info(result.toString)
          assert(result.isRight, result)
        }
      }
    }
  }
}
