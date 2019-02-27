package io.hydrosphere.serving.manager.domain

import cats.Id
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.domain.host_selector.{HostSelector, HostSelectorRepository, HostSelectorService}
import org.mockito.{Matchers, Mockito}

import scala.concurrent.Future

class HostSelectorServiceSpec extends GenericUnitTest {
  describe("Environment management service") {
    it("should return an environment by id") {
      val envRepo = mock[HostSelectorRepository[Id]]

      Mockito.when(envRepo.get("test")).thenReturn(
        Some(
          HostSelector(1, "test", "placeholder")
        )
      )

      val environmentService = HostSelectorService(envRepo)

      val res = environmentService.get("test")
      assert(res.isRight, res)
      val env = res.right.get
      assert(env.name === "test")
      assert(env.id === 1)
      assert(env.placeholder === "placeholder")
    }

    it("should create a new environment") {
      val envRepo = mock[HostSelectorRepository[Id]]

      Mockito.when(envRepo.get("new_test")).thenReturn(
        None
      )
      val hostSelector = HostSelector(
        1,
        "new_test",
        "placeholder"
      )
      when(envRepo.create(Matchers.any())).thenReturn(hostSelector)

      val environmentService = HostSelectorService(envRepo)

      val res = environmentService.create("new_test", "placeholder")
      assert(res.isRight, res)
      val env = res.right.get
      assert(env.name === "new_test")
      assert(env.placeholder === "placeholder")

    }

    it("should reject a creation of duplicate environments") {
      val envRepo = mock[HostSelectorRepository[Id]]

      Mockito.when(envRepo.get("new_test")).thenReturn(
        Some(HostSelector(1, "new_test", "placeholder"))
      )

      val environmentService = HostSelectorService(envRepo)
      val res = environmentService.create("new_test", "placeholder")
      assert(res.isLeft, res)
    }
  }
}
