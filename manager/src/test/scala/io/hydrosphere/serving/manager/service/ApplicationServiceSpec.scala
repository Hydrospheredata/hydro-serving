package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.api.ModelType.Tensorflow
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.repository.{ApplicationRepository, RuntimeRepository}
import io.hydrosphere.serving.manager.service.application.ApplicationManagementServiceImpl
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.manager.service.runtime.RuntimeManagementService
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus
import org.mockito.Matchers

import scala.concurrent.{Await, ExecutionContext, Future}

class ApplicationServiceSpec extends GenericUnitTest {

  implicit val ctx = ExecutionContext.global

  describe("Application management service") {
    it("should be enriched") {
      val models = Map(1l -> ModelVersion(
        id = 1,
        imageName = "",
        imageTag = "",
        imageSHA256 = "",
        created = LocalDateTime.now(),
        modelName = "model_name",
        modelVersion = 1,
        modelType = Tensorflow("1.1.0"),
        model = None,
        modelContract = ModelContract()
      ))
      val runtime = Map(1l -> Runtime(
        id = 1,
        name = "runtime",
        version = "latest",
        suitableModelType = List(),
        tags = List(),
        configParams = Map()
      ))
      val application: Application = Application(
        id = 1,
        name = "app",
        contract = ModelContract(),
        kafkaStreaming = List(),
        namespace = None,
        executionGraph = ApplicationExecutionGraph(
          stages = List(
            ApplicationStage(
              signature = None,
              services = List(
                WeightedService(
                  weight = 100,
                  signature = None,
                  serviceDescription = ServiceKeyDescription(
                    1, Some(1), None
                  )
                )
              )
            )
          )
        )
      )

      val appService = new ApplicationManagementServiceImpl(
        null, null, null, null, null, null, null, null, null
      )

      val modelsMap = Future.successful(models)
      val runtimeMap = Future.successful(runtime)
      val apps = Seq(application)

      import scala.concurrent.duration._

      val result = Await.result(appService.enrichServiceKeyDescription(apps, runtimeMap, modelsMap), 1 second)
      val serviceDescription = result.head.executionGraph.stages.head.services.head.serviceDescription
      assert(serviceDescription.runtimeName.contains("runtime:latest"))
      assert(serviceDescription.modelName.contains("model_name:1"))
    }

    it("should find version usage in applications") {
      val createdTime = LocalDateTime.now()

      val unbuiltModel = Model(1, "model1", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime)

      val builtModel1 = Model(2, "model2", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime)
      val mVersion1 = ModelVersion(1, "image", "tag", "sha256", createdTime, builtModel1.name, 1, ModelType.Tensorflow("1.1.0"), Some(builtModel1), builtModel1.modelContract)
      val mBuild1 = ModelBuild(1, builtModel1, 1, createdTime, Some(LocalDateTime.now()), ServiceTaskStatus.Finished, None, None, Some(mVersion1), "")

      val builtModel2 = Model(3, "model3", ModelType.Tensorflow("1.1.0"), None, ModelContract.defaultInstance, createdTime, createdTime)
      val mVersion2 = ModelVersion(2, "image", "tag", "sha256", createdTime, builtModel2.name, 1, ModelType.Tensorflow("1.1.0"), Some(builtModel2), builtModel2.modelContract)
      val mBuild2 = ModelBuild(2, builtModel2, 1, createdTime, Some(LocalDateTime.now()), ServiceTaskStatus.Finished, None, None, Some(mVersion2), "")

      val graph1 = ApplicationExecutionGraph(
        List(
          ApplicationStage(
            List(WeightedService(
              ServiceKeyDescription(1, Some(mVersion1.id), None, Some(mVersion1.fullName), None),
              100,
              None
            )), None
          ),
          ApplicationStage(
            List(WeightedService(
              ServiceKeyDescription(2, Some(mVersion2.id), None, Some(mVersion2.fullName), None),
              100,
              None
            )), None
          )
        )
      )
      val app1 = Application(1, "testapp1", None, ModelContract.defaultInstance, graph1, List.empty)
      val graph2 = ApplicationExecutionGraph(
        List(ApplicationStage(
          List(
            WeightedService(
              ServiceKeyDescription(1, Some(mVersion1.id), None, Some(mVersion1.fullName), None),
              100,
              None
            ),
            WeightedService(
              ServiceKeyDescription(1, Some(mVersion2.id), None, Some(mVersion2.fullName), None),
              100,
              None
            )
          ), None
        ))
      )
      val app2 = Application(2, "testapp2", None, ModelContract.defaultInstance, graph2, List.empty)
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
      val app3 = Application(3, "testapp3", None, ModelContract.defaultInstance, graph3, List.empty)

      val appRepo = mock[ApplicationRepository]
      when(appRepo.all()).thenReturn(Future.successful(Seq(app1, app2, app3)))

      val versionMock = mock[ModelVersionManagementService]
      when(versionMock.modelVersionsByModelVersionIds(Matchers.any())).thenReturn(Future.successful(Seq(mVersion1, mVersion2)))

      val runtimeRepo = mock[RuntimeRepository]
      when(runtimeRepo.all()).thenReturn(Future.successful(Seq.empty))

      val service = new ApplicationManagementServiceImpl(
        appRepo, versionMock, null, null, null, null, null, null, runtimeRepo
      )
      for {
        a1 <- service.findVersionUsage(1)
        a2 <- service.findVersionUsage(2)
      } yield {
        assert(a1.nonEmpty, 1)
        assert(a1.exists(_.name === "testapp1"))
        assert(a1.exists(_.name === "testapp2"))
        assert(!a1.exists(_.name === "testapp3"))

        assert(a2.nonEmpty, 2)
        assert(a2.exists(_.name === "testapp1"))
        assert(a2.exists(_.name === "testapp2"))
        assert(!a2.exists(_.name === "testapp3"))

      }
    }
  }
}