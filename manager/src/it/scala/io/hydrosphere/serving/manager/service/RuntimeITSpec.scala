package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.test.FullIntegrationSpec

class RuntimeITSpec extends FullIntegrationSpec {
  "Runtime service" should {
    "add a runtime" when {
      "runtime is supported" in {
        val request = CreateRuntimeRequest(
          name = "known_test",
          version = "0.0.1",
          modelTypes = Some(List("spark:2.1")),
          tags = None,
          configParams = None
        )
        managerServices.runtimeManagementService.create(request).map{ runtime =>
          assert(request.name === runtime.name, runtime.name)
          assert(runtime.suitableModelType.lengthCompare(1) === 0, request)
          assert(ModelType.fromTag(request.modelTypes.get.head) === runtime.suitableModelType.head)
          assert(Map.empty[String, String] === runtime.configParams)
          assert(request.version === runtime.version)
          assert(List.empty[String] === runtime.tags)
        }
      }
      "runtime is unsupported" in {
        val request = CreateRuntimeRequest(
          name = "unknown_test",
          version = "0.0.1",
          modelTypes = Some(List("tensorLUL:1337")),
          tags = None,
          configParams = None
        )
        managerServices.runtimeManagementService.create(request).map{ runtime =>
          assert(request.name === runtime.name, runtime.name)
          assert(runtime.suitableModelType.lengthCompare(1) === 0, request)
          assert(ModelType.Unknown("tensorLUL:1337") === runtime.suitableModelType.head)
          assert(Map.empty[String, String] === runtime.configParams)
          assert(request.version === runtime.version)
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
