package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.controller.model_source.AddLocalSourceRequest
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.LocalSourceParams
import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.{GenericUnitTest, ManagerConfiguration}

import scala.concurrent.Await

class SourceManagementSpecs extends GenericUnitTest {

  import scala.concurrent.ExecutionContext.Implicits._

  "Source management service" should "index specified source path" in {
    val confMock = mock[ManagerConfiguration]
    val sourceRepoMock = mock[SourceConfigRepository]

    val sourceService = new SourceManagementServiceImpl(confMock, sourceRepoMock)
    val f = sourceService.index("test:kek").map { result =>
      println(result)
    }

    Await.result(f, futureTimeout)
  }

  it should "list all (config+db) sources" in {
    val confMock = mock[ManagerConfiguration]
    val sourceRepoMock = mock[SourceConfigRepository]

    val sourceService = new SourceManagementServiceImpl(confMock, sourceRepoMock)
    val f = sourceService.allSourceConfigs.map { result =>
      println(result)
    }

    Await.result(f, futureTimeout)
  }

  it should "add a local source" in {
    val confMock = mock[ManagerConfiguration]
    val sourceRepoMock = mock[SourceConfigRepository]

    val sourceService = new SourceManagementServiceImpl(confMock, sourceRepoMock)

    val req = AddLocalSourceRequest(
      "test_api", getClass.getResource("/models").getPath
    )
    sourceService.addLocalSource(req).map { maybeSourceConfig =>
      assert(maybeSourceConfig.isDefined)
      val modelSourceConfig = maybeSourceConfig.get
      assert(modelSourceConfig.name === req.name)
      assert(modelSourceConfig.params.isInstanceOf[LocalSourceParams], modelSourceConfig.params)
      assert(modelSourceConfig.params.asInstanceOf[LocalSourceParams].pathPrefix.get === req.path)
    }
  }

  it should "reject similar source" in {
    val confMock = mock[ManagerConfiguration]
    val sourceRepoMock = mock[SourceConfigRepository]

    val sourceService = new SourceManagementServiceImpl(confMock, sourceRepoMock)

    val reqSuccess = AddLocalSourceRequest(
      "test", getClass.getResource("/models").getPath
    )
    val reqFail = AddLocalSourceRequest(
      "test", getClass.getResource("/models").getPath
    )
    for {
      successSource <- sourceService.addLocalSource(reqSuccess)
      failSource <- sourceService.addLocalSource(reqFail)
    } yield {
      assert(successSource.isDefined)
      assert(failSource.isEmpty)
    }
  }

  it should "list all sources (config+db)" in {
    val confMock = mock[ManagerConfiguration]
    val sourceRepoMock = mock[SourceConfigRepository]

    val sourceService = new SourceManagementServiceImpl(confMock, sourceRepoMock)

    sourceService.allSourceConfigs.map { sources =>
      println(sources)
      assert(sources.exists(_.name == "test_config"))
      assert(sources.exists(_.name == "test_api"))
      assert(sources.exists(_.name == "test"))
    }
  }
}