package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.repository.SourceConfigRepository
import io.hydrosphere.serving.manager.{GenericUnitTest, ManagerConfiguration}

import scala.concurrent.Await

class SourceManagementSpecs extends GenericUnitTest {
  import scala.concurrent.ExecutionContext.Implicits._

  "Source management service" should "index specified source path" in {
    val confMock = mock[ManagerConfiguration]
    val sourceRepoMock = mock[SourceConfigRepository]

    val sourceService = new SourceManagementServiceImpl(confMock, sourceRepoMock)
    val f = sourceService.index("test:kek").map{ result =>
      println(result)
    }

    Await.result(f, futureTimeout)
  }

  it should "list all (config+db) sources" in {
    val confMock = mock[ManagerConfiguration]
    val sourceRepoMock = mock[SourceConfigRepository]

    val sourceService = new SourceManagementServiceImpl(confMock, sourceRepoMock)
    val f = sourceService.allSourceConfigs.map{ result =>
      println(result)
    }

    Await.result(f, futureTimeout)
  }
}
