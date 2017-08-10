package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, TestConstants}
import io.hydrosphere.serving.manager.service.modelsource.LocalModelSource
import org.scalatest.{FlatSpec, Matchers}

class SourceManagementServiceSpecs extends FlatSpec with Matchers {
  val localSource = new LocalModelSource(LocalModelSourceConfiguration("test", TestConstants.localModelsPath))

  "SourceManagementService" should "" in {

  }
}
