package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.controller.model_source.AddLocalSourceRequest
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.LocalSourceParams
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.service.source.SourceManagementServiceImpl
import io.hydrosphere.serving.manager.{GenericUnitTest, ManagerConfiguration}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.{Matchers, Mockito}

import scala.concurrent.{Await, Future}

class SourceServiceSpec extends GenericUnitTest {

  import scala.concurrent.ExecutionContext.Implicits._

  "Source management service" should "index specified source path" in {
    val confMock = mock[ManagerConfiguration]
    val sourceRepoMock = mock[SourceConfigRepository]

    Mockito.when(confMock.modelSources).thenReturn(
      Seq(
        ModelSourceConfig(1, "test", LocalSourceParams(Some(getClass.getResource("/test_models").getPath)))
      )
    )
    Mockito.when(sourceRepoMock.all()).thenReturn(Future.successful(Seq.empty))

    val sourceService = new SourceManagementServiceImpl(confMock, sourceRepoMock)

    sourceService.index("test:tensorflow_model").map { result =>
      val maybeModel = result.right.get
      println(maybeModel)
      maybeModel shouldBe defined
      val model = maybeModel.get
      println(model)
      model.modelName should equal("tensorflow_model")
    }
  }

  it should "list all (config+db) sources" in {
    val confMock = mock[ManagerConfiguration]
    val sourceRepoMock = mock[SourceConfigRepository]

    val s1 =  ModelSourceConfig(1, "test", LocalSourceParams(Some(getClass.getResource("/test_models").getPath)))
    val s2 =  ModelSourceConfig(2, "test2", LocalSourceParams(Some(getClass.getResource("/test_models").getPath)))

    Mockito.when(confMock.modelSources).thenReturn(Seq(s1))
    Mockito.when(sourceRepoMock.all()).thenReturn(Future.successful(Seq(s2)))

    val sourceService = new SourceManagementServiceImpl(confMock, sourceRepoMock)
    sourceService.allSourceConfigs.map { result =>
      println(result)
      result should contain allOf(s1, s2)
    }
  }

  it should "add a local source" in {
    val confMock = mock[ManagerConfiguration]
    val sourceRepoMock = mock[SourceConfigRepository]

    Mockito.when(confMock.modelSources).thenReturn(Seq.empty)
    Mockito.when(sourceRepoMock.all()).thenReturn(Future.successful(Seq.empty))
    Mockito.when(sourceRepoMock.create(Matchers.any())).thenAnswer(new Answer[Future[ModelSourceConfig]] {
      override def answer(invocation: InvocationOnMock): Future[ModelSourceConfig] = {
        val s = invocation.getArguments.head.asInstanceOf[ModelSourceConfig]
        Future.successful(s)
      }
    })

    val sourceService = new SourceManagementServiceImpl(confMock, sourceRepoMock)

    val req = AddLocalSourceRequest(
      "test_api", getClass.getResource("/test_models").getPath
    )
    sourceService.addLocalSource(req).map { maybeSourceConfig =>
      maybeSourceConfig.isRight should equal(true)
      val modelSourceConfig = maybeSourceConfig.right.get
      modelSourceConfig.name should equal(req.name)

      assert(modelSourceConfig.params.isInstanceOf[LocalSourceParams], modelSourceConfig.params)
      assert(modelSourceConfig.params.asInstanceOf[LocalSourceParams].pathPrefix.get === req.path)
    }
  }

  it should "reject similar source" in {
    val confMock = mock[ManagerConfiguration]
    val sourceRepoMock = mock[SourceConfigRepository]

    val sourceService = new SourceManagementServiceImpl(confMock, sourceRepoMock)

    val s1 = ModelSourceConfig(1, "test", LocalSourceParams(Some(getClass.getResource("/test_models").getPath)))

    Mockito.when(confMock.modelSources).thenReturn(Seq(s1))
    Mockito.when(sourceRepoMock.all()).thenReturn(Future.successful(Seq.empty))

    val reqFail = AddLocalSourceRequest(
      s1.name, "I MUST FAIL"
    )
    for {
      failSource <- sourceService.addLocalSource(reqFail)
    } yield {
      assert(failSource.isLeft)
    }
  }
}