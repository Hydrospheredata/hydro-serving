package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.repository.RuntimeRepository
import io.hydrosphere.serving.manager.service.runtime.{RuntimeManagementService, RuntimeManagementServiceImpl}

class RuntimeServiceSpec extends GenericUnitTest {
  "Runtime service" should "add supported runtime" in {
    val runtimeRepo = mock[RuntimeRepository]
    val runtimeManagementService = new RuntimeManagementServiceImpl(runtimeRepo)
    runtimeManagementService.create(
      name = "known_test",
      version = "0.0.1",
      modelTypes = List("spark:2.1"),
      tags = List.empty,
      configParams = Map.empty
    ).map { runtime =>
      assert("known_test" === runtime.name, runtime.name)
      assert(runtime.suitableModelType.lengthCompare(1) === 0, "spark:2.1")
      assert(ModelType.fromTag("spark:2.1") === runtime.suitableModelType.head)
      assert(Map.empty[String, String] === runtime.configParams)
      assert("0.0.1" === runtime.version)
      assert(List.empty[String] === runtime.tags)
    }
  }

  it should "add unsupported runtime" in {
    val runtimeRepo = mock[RuntimeRepository]
    val runtimeManagementService = new RuntimeManagementServiceImpl(runtimeRepo)
    runtimeManagementService.create(
      name = "unknown_test",
      version = "0.0.1",
      modelTypes = List("tensorLUL:1337"),
      tags = List.empty,
      configParams = Map.empty
    ).map { runtime =>
      assert("unknown_test" === runtime.name, runtime.name)
      assert(runtime.suitableModelType.lengthCompare(1) === 0, "tensorLUL:1337")
      assert(ModelType.Unknown("tensorLUL:1337") === runtime.suitableModelType.head)
      assert(Map.empty[String, String] === runtime.configParams)
      assert("0.0.1" === runtime.version)
      assert(List.empty[String] === runtime.tags)
    }
  }

  it should "reject addition of existing runtime" in {
    pending
  }

  it should "list all runtimes" in {
    pending
  }
}
