package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.api.ModelType.Tensorflow
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.repository.{ApplicationRepository, RuntimeRepository}
import io.hydrosphere.serving.manager.service.application.ApplicationManagementServiceImpl
import io.hydrosphere.serving.manager.service.environment.AnyEnvironment
import io.hydrosphere.serving.manager.service.model_version.ModelVersionManagementService
import io.hydrosphere.serving.manager.service.runtime.RuntimeManagementService
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus
import org.mockito.Matchers

import scala.concurrent.{Await, ExecutionContext, Future}

class ApplicationServiceSpec extends GenericUnitTest {

  implicit val ctx = ExecutionContext.global

  describe("Application management service") {
    it("should find version usage in applications") {
      val createdTime = LocalDateTime.now()

      val runtime = Runtime(1, "name", "version", List.empty, List.empty, Map.empty)

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
            List(DetailedServiceDescription(
              weight = 100,
              signature = None,
              runtime = runtime,
              modelVersion = mVersion1,
              environment = AnyEnvironment
            )),
            None,
            Map.empty
          ),
          ApplicationStage(
            List(DetailedServiceDescription(
              weight = 100,
              signature = None,
              runtime = runtime,
              modelVersion = mVersion2,
              environment = AnyEnvironment
            )),
            None,
            Map.empty
          )
        )
      )
      val app1 = Application(1, "testapp1", None, ModelContract.defaultInstance, graph1, List.empty)
      val graph2 = ApplicationExecutionGraph(
        List(ApplicationStage(
          List(
            DetailedServiceDescription(
              weight = 100,
              signature = None,
              runtime = runtime,
              modelVersion = mVersion1,
              environment = AnyEnvironment
            ),
            DetailedServiceDescription(
              weight = 100,
              signature = None,
              runtime = runtime,
              modelVersion = mVersion2,
              environment = AnyEnvironment
            )
          ), None, Map.empty
        ))
      )
      val app2 = Application(2, "testapp2", None, ModelContract.defaultInstance, graph2, List.empty)
      val graph3 = ApplicationExecutionGraph(
        List(ApplicationStage(
          List.empty, None, Map.empty
        ))
      )
      val app3 = Application(3, "testapp3", None, ModelContract.defaultInstance, graph3, List.empty)

      val appRepo = mock[ApplicationRepository]
      when(appRepo.all()).thenReturn(Future.successful(Seq(app1, app2, app3)))

      val versionMock = mock[ModelVersionManagementService]
      when(versionMock.modelVersionsByModelVersionIds(Matchers.any())).thenReturn(Future.successful(Seq(mVersion1, mVersion2)))

      val runtimeServiceMock = mock[RuntimeManagementService]
      when(runtimeServiceMock.all()).thenReturn(Future.successful(Seq.empty))

      val service = new ApplicationManagementServiceImpl(
        appRepo, versionMock, null, null, null, null, null, null, null, runtimeServiceMock
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