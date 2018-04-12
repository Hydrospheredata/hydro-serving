package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.controller.runtime.CreateRuntimeRequest
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.test.FullIntegrationSpec

// TODO move to unittest
class RuntimeITSpec extends FullIntegrationSpec {
  "Runtime service" should {
    "add a runtime" when {
      "runtime is supported" in {
        managerServices.runtimeManagementService.create(
          name = "known_test",
          version = "0.0.1",
          modelTypes = List("spark:2.1"),
          tags = List.empty,
          configParams = Map.empty
        ).map{ runtime =>
          assert("known_test" === runtime.name, runtime.name)
          assert(runtime.suitableModelType.lengthCompare(1) === 0, "spark:2.1")
          assert(ModelType.fromTag("spark:2.1") === runtime.suitableModelType.head)
          assert(Map.empty[String, String] === runtime.configParams)
          assert("0.0.1" === runtime.version)
          assert(List.empty[String] === runtime.tags)
        }
      }
      "runtime is unsupported" in {
        managerServices.runtimeManagementService.create(
          name = "unknown_test",
          version = "0.0.1",
          modelTypes = List("tensorLUL:1337"),
          tags = List.empty,
          configParams = Map.empty
        ).map{ runtime =>
          assert("unknown_test" === runtime.name, runtime.name)
          assert(runtime.suitableModelType.lengthCompare(1) === 0, "tensorLUL:1337")
          assert(ModelType.Unknown("tensorLUL:1337") === runtime.suitableModelType.head)
          assert(Map.empty[String, String] === runtime.configParams)
          assert("0.0.1" === runtime.version)
          assert(List.empty[String] === runtime.tags)
        }
      }
    }

    "list runtimes" in {
      managerServices.runtimeManagementService.all().map { runtimes =>
        assert(runtimes.lengthCompare(3) === 0, runtimes)
      }
    }
  }
}
