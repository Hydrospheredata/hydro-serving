package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.model.ModelBuildStatus
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.service.aggregated_info.AggregatedInfoUtilityServiceImpl
import io.hydrosphere.serving.manager.service.application.ApplicationManagementService
import io.hydrosphere.serving.manager.service.model.ModelManagementService
import io.hydrosphere.serving.manager.service.model_build.ModelBuildManagmentService
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import org.mockito.{Matchers, Mockito}

import scala.concurrent.Future

class AggregatedInfoServiceSpec extends GenericUnitTest {

  "AggregatedInfoService" should "return info for ready to build models" in {
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

    aggService.allModelsAggregatedInfo().map{ result =>
      assert(result.nonEmpty)
      assert(result.forall(_.lastModelBuild.isEmpty), result)
      assert(result.forall(_.lastModelVersion.isEmpty), result)
      assert(result.forall(_.nextVersion.isDefined))
      val iModels = result.map(_.model)
      assert(iModels.exists(_.name == "model1"))
      assert(iModels.exists(_.name == "model2"))
    }
  }

  it should "return info for built models" in {
    val createdTime = LocalDateTime.now()

    val unbuiltModel = Model(2, "model2", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime)
    val builtModel = Model(1, "model1", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime)
    val mVersion = ModelVersion(1, "image", "tag", "sha256", createdTime, builtModel.name, 1, ModelType.Tensorflow("1.1.0"), Some(builtModel), builtModel.modelContract)
    val mBuild = ModelBuild(1, builtModel, 1, createdTime, Some(LocalDateTime.now()), ModelBuildStatus.FINISHED, None, None, Some(mVersion))

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

    aggService.allModelsAggregatedInfo().map{ result =>
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

  it should "return  apps info for model versions" in {
    val createdTime = LocalDateTime.now()

    val unbuiltModel = Model(1, "model1", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime)

    val builtModel1 = Model(2, "model2", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime)
    val mVersion1 = ModelVersion(1, "image", "tag", "sha256", createdTime, builtModel1.name, 1, ModelType.Tensorflow("1.1.0"), Some(builtModel1), builtModel1.modelContract)
    val mBuild1 = ModelBuild(1, builtModel1, 1, createdTime, Some(LocalDateTime.now()), ModelBuildStatus.FINISHED, None, None, Some(mVersion1))

    val builtModel2 = Model(3, "model3", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime)
    val mVersion2 = ModelVersion(2, "image", "tag", "sha256", createdTime, builtModel2.name, 1, ModelType.Tensorflow("1.1.0"), Some(builtModel2), builtModel2.modelContract)
    val mBuild2 = ModelBuild(2, builtModel2, 1, createdTime, Some(LocalDateTime.now()), ModelBuildStatus.FINISHED, None, None, Some(mVersion2))


    val graph1 = ApplicationExecutionGraph(
      List(
        ApplicationStage(
          List(WeightedService(
              ServiceKeyDescription(1,Some(mVersion1.id), None, Some(mVersion1.fullName), None),
              100,
              None
            )), None
        ),
        ApplicationStage(
          List(WeightedService(
            ServiceKeyDescription(2,Some(mVersion2.id), None, Some(mVersion2.fullName), None),
            100,
            None
          )), None
        )
      )
    )
    val app1 = Application(1, "testapp1", ModelContract.defaultInstance, graph1, List.empty)
    val graph2 = ApplicationExecutionGraph(
      List(ApplicationStage(
        List(
          WeightedService(
          ServiceKeyDescription(1,Some(mVersion1.id), None, Some(mVersion1.fullName), None),
          100,
          None
        ),
          WeightedService(
            ServiceKeyDescription(1,Some(mVersion2.id), None, Some(mVersion2.fullName), None),
            100,
            None
          )
        ), None
      ))
    )
    val app2 = Application(2, "testapp2", ModelContract.defaultInstance, graph2, List.empty)
    val graph3 = ApplicationExecutionGraph(
      List(ApplicationStage(
        List(
          WeightedService(
            ServiceKeyDescription(1, None, None, None, None),
            100,
            None
          )
        ), None
      ))
    )
    val app3 = Application(3, "testapp3", ModelContract.defaultInstance, graph3, List.empty )

    val modelMock = mock[ModelManagementService]
    Mockito.when(modelMock.allModels()).thenReturn(
      Future.successful(Seq(unbuiltModel, builtModel1, builtModel2))
    )

    val buildMock = mock[ModelBuildManagmentService]

    val versionMock = mock[ModelVersionManagementService]
    Mockito.when(versionMock.list).thenReturn(
      Future.successful(Seq(mVersion1, mVersion2))
    )

    val appMock = mock[ApplicationManagementService]
    Mockito.when(appMock.allApplications()).thenReturn(
      Future.successful(Seq(app1, app2, app3))
    )

    val aggService = new AggregatedInfoUtilityServiceImpl(modelMock, buildMock, versionMock, appMock)

    aggService.allModelVersions.map{ result =>
      assert(result.nonEmpty)
      val m2 = result.find(_.modelName == "model2").get
      val m3 = result.find(_.modelName == "model3").get

      assert(m2.model.get === builtModel1)
      assert(m2.applications === Seq(app1, app2))

      assert(m3.model.get === builtModel2)
      assert(m3.applications === Seq(app1, app2))
    }
  }

}
