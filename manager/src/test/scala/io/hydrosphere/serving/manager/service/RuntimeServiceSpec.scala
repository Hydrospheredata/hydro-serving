package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.Runtime
import io.hydrosphere.serving.manager.repository.RuntimeRepository
import io.hydrosphere.serving.manager.service.runtime.RuntimeManagementServiceImpl
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.{Matchers, Mockito}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

class RuntimeServiceSpec extends GenericUnitTest {
  describe("Runtime service") {
    describe("addition") {
      describe("succeeds") {
        it("with supported runtime") {
          val runtimeRepo = mock[RuntimeRepository]
          Mockito.when(runtimeRepo.create(Matchers.any())).thenAnswer(new Answer[Future[Runtime]] {
            override def answer(invocation: InvocationOnMock): Future[Runtime] = {
              Future.successful(
                invocation.getArgumentAt(0, classOf[Runtime])
              )
            }
          })
          Mockito.when(runtimeRepo.fetchByNameAndVersion(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
          val runtimeManagementService = new RuntimeManagementServiceImpl(runtimeRepo)
          runtimeManagementService.create(
            name = "known_test",
            version = "0.0.1",
            modelTypes = List("spark:2.1"),
            tags = List.empty,
            configParams = Map.empty
          ).map { runtimeResult =>
            val runtime = runtimeResult.right.get
            assert("known_test" === runtime.name, runtime.name)
            assert(runtime.suitableModelType.lengthCompare(1) === 0, "spark:2.1")
            assert(ModelType.fromTag("spark:2.1") === runtime.suitableModelType.head)
            assert(Map.empty[String, String] === runtime.configParams)
            assert("0.0.1" === runtime.version)
            assert(List.empty[String] === runtime.tags)
          }
        }
        it("with unsupported runtime") {
          val runtimeRepo = mock[RuntimeRepository]
          Mockito.when(runtimeRepo.create(Matchers.any())).thenAnswer(new Answer[Future[Runtime]] {
            override def answer(invocation: InvocationOnMock): Future[Runtime] = {
              Future.successful(
                invocation.getArgumentAt(0, classOf[Runtime])
              )
            }
          })
          Mockito.when(runtimeRepo.fetchByNameAndVersion(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))

          val runtimeManagementService = new RuntimeManagementServiceImpl(runtimeRepo)
          runtimeManagementService.create(
            name = "unknown_test",
            version = "0.0.1",
            modelTypes = List("tensorLUL:1337"),
            tags = List.empty,
            configParams = Map.empty
          ).map { runtimeResult =>
            val runtime = runtimeResult.right.get
            assert("unknown_test" === runtime.name, runtime.name)
            assert(runtime.suitableModelType.lengthCompare(1) === 0, "tensorLUL:1337")
            assert(ModelType.Unknown("tensorLUL", "1337") === runtime.suitableModelType.head)
            assert(Map.empty[String, String] === runtime.configParams)
            assert("0.0.1" === runtime.version)
            assert(List.empty[String] === runtime.tags)
          }
        }
      }
      describe("fails") {
        it("with already existing runtime") {
          val runtimeRepo = mock[RuntimeRepository]
          Mockito.when(runtimeRepo.fetchByNameAndVersion(Matchers.any(), Matchers.any())).thenReturn(
            Future.successful(Some(
              Runtime(
                0, "test", "latest", List.empty, List.empty, Map.empty
              )
            )
            )
          )

          val runtimeManagementService = new RuntimeManagementServiceImpl(runtimeRepo)
          runtimeManagementService.create(
            name = "test",
            version = "latest",
            modelTypes = List("tensorLUL:1337"),
            tags = List.empty,
            configParams = Map.empty
          ).map { runtimeResult =>
            assert(runtimeResult.isLeft, runtimeResult)
          }
        }
      }
    }

    it("lists all runtimes") {
      val runtimeRepo = mock[RuntimeRepository]
      Mockito.when(runtimeRepo.all()).thenAnswer(new Answer[Future[Seq[Runtime]]] {
        override def answer(invocation: InvocationOnMock): Future[Seq[Runtime]] = {
          Future.successful(
            Seq(
              Runtime(0, "test1", "latest", List.empty, List.empty, Map.empty),
              Runtime(1, "test2", "latest", List.empty, List.empty, Map.empty)
            )
          )
        }
      })

      val runtimeManagementService = new RuntimeManagementServiceImpl(runtimeRepo)
      runtimeManagementService.all().map { runtimes =>
        println(runtimes)
        assert(runtimes.exists(_.name == "test2"))
        assert(runtimes.exists(_.name == "test1"))
      }
    }
  }
}
