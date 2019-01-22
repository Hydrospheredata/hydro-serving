package io.hydrosphere.serving.manager.domain

import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.domain.host_selector.{HostSelector, HostSelectorRepositoryAlgebra, HostSelectorService}
import org.mockito.{Matchers, Mockito}

import scala.concurrent.Future

class HostSelectorServiceSpec extends GenericUnitTest {
  describe("Environment management service") {
    it("should return all environments") {
      val envRepo = mock[HostSelectorRepositoryAlgebra[Future]]

      Mockito.when(envRepo.all()).thenReturn(
        Future.successful(
          Seq(
            HostSelector(1, "test", "placeholder")
          )
        )
      )

      val environmentService = new HostSelectorService(envRepo)

      environmentService.all().map { envs =>
        assert(envs.exists(_.name == "test"))
      }
    }

    it("should return an environment by id") {
      val envRepo = mock[HostSelectorRepositoryAlgebra[Future]]

      Mockito.when(envRepo.get(1L)).thenReturn(
        Future.successful(
          Some(
            HostSelector(1, "test", "placeholder")
          )
        )
      )

      val environmentService = new HostSelectorService(envRepo)

      environmentService.get(1).map { res =>
        assert(res.isRight, res)
        val env = res.right.get
        assert(env.name === "test")
        assert(env.id === 1)
        assert(env.placeholder === "placeholder")
      }
    }

    it("should create a new environment") {
      val envRepo = mock[HostSelectorRepositoryAlgebra[Future]]

      Mockito.when(envRepo.get("new_test")).thenReturn(
        Future.successful(None)
      )
      val hostSelector = HostSelector(
        1,
        "new_test",
        "placeholder"
      )
      when(envRepo.create(Matchers.any())).thenReturn(Future.successful(hostSelector))

      val environmentService = new HostSelectorService(envRepo)

      environmentService.create("new_test", "placeholder").map { res =>
        assert(res.isRight, res)
        val env = res.right.get
        assert(env.name === "new_test")
        assert(env.placeholder === "placeholder")
      }
    }

    it("should reject a creation of duplicate environments") {
      val envRepo = mock[HostSelectorRepositoryAlgebra[Future]]

      Mockito.when(envRepo.get("new_test")).thenReturn(
        Future.successful(Some(HostSelector(1, "new_test", "placeholder")))
      )

      val environmentService = new HostSelectorService(envRepo)
      environmentService.create("new_test", "placeholder").map { res =>
        assert(res.isLeft, res)
      }
    }
  }
}
