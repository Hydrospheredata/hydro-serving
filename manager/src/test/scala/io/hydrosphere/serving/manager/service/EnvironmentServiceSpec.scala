package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.model.db.Environment
import io.hydrosphere.serving.manager.repository.EnvironmentRepository
import io.hydrosphere.serving.manager.service.environment.{AnyEnvironment, EnvironmentManagementServiceImpl}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.{Matchers, Mockito}

import scala.concurrent.Future

class EnvironmentServiceSpec extends GenericUnitTest {
  "Environment management service" should "return all environments" in {
    val envRepo = mock[EnvironmentRepository]

    Mockito.when(envRepo.all()).thenReturn(
      Future.successful(
        Seq(
          Environment(1, "test", Seq.empty)
        )
      )
    )

    val environmentService = new EnvironmentManagementServiceImpl(envRepo)

    environmentService.all().map{ envs =>
      assert(envs.contains(AnyEnvironment))
      assert(envs.exists(_.name == "test"))
    }
  }

  it should "return an environment by id" in {
    val envRepo = mock[EnvironmentRepository]

    Mockito.when(envRepo.get(1L)).thenReturn(
      Future.successful(
        Some(
          Environment(1, "test", Seq.empty)
        )
      )
    )

    val environmentService = new EnvironmentManagementServiceImpl(envRepo)

    environmentService.get(1).map{ res =>
      assert(res.isRight, res)
      val env = res.right.get
      assert(env.name === "test")
      assert(env.id === 1)
      assert(env.placeholders === Seq.empty)
    }
  }

  it should "create a new environment" in {
    val envRepo = mock[EnvironmentRepository]

    Mockito.when(envRepo.get("new_test")).thenReturn(
      Future.successful(None)
    )
    Mockito.when(envRepo.create(Matchers.any())).thenAnswer(new Answer[Future[Environment]] {
      override def answer(invocation: InvocationOnMock): Future[Environment] = {
        val arg = invocation.getArguments.head.asInstanceOf[Environment]
        Future.successful(arg.copy(id = 1))
      }
    })

    val environmentService = new EnvironmentManagementServiceImpl(envRepo)

    environmentService.create("new_test", Seq.empty).map{ res =>
      assert(res.isRight, res)
      val env = res.right.get
      assert(env.name === "new_test")
      assert(env.placeholders === Seq.empty)
    }
  }

  it should "reject a creation of duplicate environments" in {
    val envRepo = mock[EnvironmentRepository]

    Mockito.when(envRepo.get("new_test")).thenReturn(
      Future.successful(Some(Environment(1, "new_test", Seq.empty)))
    )

    val environmentService = new EnvironmentManagementServiceImpl(envRepo)
    environmentService.create("new_test", Seq.empty).map { res =>
      assert(res.isLeft, res)
    }
  }
}
