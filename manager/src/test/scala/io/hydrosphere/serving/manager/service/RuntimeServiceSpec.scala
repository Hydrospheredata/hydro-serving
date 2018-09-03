package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import com.spotify.docker.client.{DockerClient, ProgressHandler}
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.{CreateRuntimeRequest, PullRuntime, Runtime}
import io.hydrosphere.serving.manager.repository.{RuntimePullRepository, RuntimeRepository}
import io.hydrosphere.serving.manager.service.runtime.{RuntimeManagementServiceImpl, RuntimePullExecutor}
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mockito.{Matchers, Mockito}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class RuntimeServiceSpec extends GenericUnitTest {
  describe("Runtime service") {
    describe("addition") {
      describe("succeeds") {
        it("with supported runtime") {
          val runtimeRepo = mock[RuntimeRepository]
          when(runtimeRepo.create(Matchers.any())).thenAnswer((invocation: InvocationOnMock) => {
            Future.successful(
              invocation.getArgumentAt(0, classOf[Runtime])
            )
          })

          val pullStatus = PullRuntime(
            id = 0,
            name = "known_test",
            version = "0.0.1",
            suitableModelTypes = List("spark:2.1"),
            tags = List.empty,
            configParams = Map.empty,
            startedAt = LocalDateTime.now(),
            finishedAt = None,
            status = ServiceTaskStatus.Pending
          )
          val runtimePullRepo = mock[RuntimePullRepository]
          when(runtimePullRepo.create(Matchers.any())).thenReturn(
            Future.successful(pullStatus.copy(id = 1))
          )
          when(runtimePullRepo.get(1)).thenReturn(Future.successful(Some(pullStatus)))
          when(runtimePullRepo.getRunningPull("known_test", "0.0.1")).thenReturn(
            Future.successful(None)
          )

          val dockerMock = mock[DockerClient]
          Mockito.doNothing().when(dockerMock).pull(Matchers.eq("known_test:0.0.1"), Matchers.any(classOf[ProgressHandler]))

          when(runtimeRepo.fetchByNameAndVersion(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
          val runtimeManagementService = new RuntimeManagementServiceImpl(runtimeRepo, runtimePullRepo, dockerMock)
          val createReq = CreateRuntimeRequest(
            name = "known_test",
            version = "0.0.1",
            modelTypes = List(ModelType.Spark("2.1").toTag),
            tags = List.empty,
            configParams = Map.empty
          )
          runtimeManagementService.create(createReq).map { runtimeResult =>
            val task = runtimeResult.right.get
            assert(task.name === createReq.name)
            assert(task.version === createReq.version)
            assert(task.configParams === createReq.configParams)
            assert(task.tags === createReq.tags)
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

          val pullStatus = PullRuntime(
            id = 0,
            name = "unknown_test",
            version = "0.0.1",
            suitableModelTypes = List("tensorLUL:1337"),
            tags = List.empty,
            configParams = Map.empty,
            startedAt = LocalDateTime.now(),
            finishedAt = None,
            status = ServiceTaskStatus.Pending
          )
          val runtimePullRepo = mock[RuntimePullRepository]
          when(runtimePullRepo.create(Matchers.any())).thenReturn(
            Future.successful(pullStatus.copy(id = 1))
          )
          when(runtimePullRepo.get(1)).thenReturn(Future.successful(Some(pullStatus)))
          when(runtimePullRepo.getRunningPull("unknown_test", "0.0.1")).thenReturn(
            Future.successful(None)
          )

          val dockerMock = mock[DockerClient]
          Mockito.doNothing().when(dockerMock).pull(Matchers.eq("unknown_test:0.0.1"), Matchers.any(classOf[ProgressHandler]))

          val runtimeManagementService = new RuntimeManagementServiceImpl(runtimeRepo, runtimePullRepo, dockerMock)
          val createReq = CreateRuntimeRequest(
            name = "unknown_test",
            version = "0.0.1",
            modelTypes = List(ModelType.Unknown("tensorLUL", "1337").toTag),
            tags = List.empty,
            configParams = Map.empty
          )
          runtimeManagementService.create(createReq).map { runtimeResult =>
            val task = runtimeResult.right.get
            assert(task.name === createReq.name)
            assert(task.version === createReq.version)
            assert(task.configParams === createReq.configParams)
            assert(task.tags === createReq.tags)

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
            ))
          )

          val runtimeManagementService = new RuntimeManagementServiceImpl(runtimeRepo, null, null)
          val createReq = CreateRuntimeRequest(
            name = "test",
            version = "latest",
            modelTypes = List(ModelType.Unknown("tensorLUL", "1337").toTag),
            tags = List.empty,
            configParams = Map.empty
          )
          runtimeManagementService.create(createReq).map { runtimeResult =>
            assert(runtimeResult.isLeft, runtimeResult)
          }
        }
      }
    }

    it("lists all runtimes") {
      val runtimeRepo = mock[RuntimeRepository]
      Mockito.when(runtimeRepo.all()).thenReturn(Future.successful(
        Seq(
          Runtime(0, "test1", "latest", List.empty, List.empty, Map.empty),
          Runtime(1, "test2", "latest", List.empty, List.empty, Map.empty)
        )
      ))

      val runtimeManagementService = new RuntimeManagementServiceImpl(runtimeRepo, null, null)
      runtimeManagementService.all().map { runtimes =>
        println(runtimes)
        assert(runtimes.exists(_.name == "test2"))
        assert(runtimes.exists(_.name == "test1"))
      }
    }
  }

  describe("RuntimePullExecutor") {
    it("should perform a pull task") {
      val pullStatus = PullRuntime(
        id = 1,
        name = "myname",
        version = "myversion",
        suitableModelTypes = List.empty,
        tags = List.empty,
        configParams = Map.empty,
        startedAt = LocalDateTime.now(),
        finishedAt = None,
        status = ServiceTaskStatus.Pending
      )

      val runtime = Runtime(
        id = 1,
        name = "myname",
        version = "myversion",
        suitableModelType = List.empty,
        tags = List.empty,
        configParams = Map.empty
      )

      val updateHistory = ListBuffer.empty[PullRuntime]

      val runtimeRepo = mock[RuntimeRepository]
      when(runtimeRepo.create(Matchers.any())).thenReturn(Future.successful(runtime))

      val pullRepo = mock[RuntimePullRepository]
      when(pullRepo.create(Matchers.any())).thenReturn(Future.successful(
        pullStatus
      ))
      when(pullRepo.get(1L)).thenReturn(Future.successful(Some(
        pullStatus
      )))
      when(pullRepo.update(Matchers.any())).thenAnswer { invocation: InvocationOnMock =>
        updateHistory += invocation.getArguments.head.asInstanceOf[PullRuntime]
        Future.successful(1)
      }

      val dockerClient = mock[DockerClient]
      val executor = new RuntimePullExecutor(pullRepo, runtimeRepo, dockerClient, this.executionContext)

      val request = CreateRuntimeRequest(
        "myname",
        "myversion"
      )
      val f = executor.execute(request)

      for {
        status <- f.taskStatus
        runtime <- f.future
      } yield {
        assert(runtime.name === "myname")
        assert(runtime.version === "myversion")

        assert(updateHistory.exists(_.status == ServiceTaskStatus.Running))
        assert(updateHistory.exists(_.status == ServiceTaskStatus.Finished))
        assert(!updateHistory.exists(_.status == ServiceTaskStatus.Failed))

        val maybePullRuntime = updateHistory.lastOption
        assert(maybePullRuntime.isDefined, maybePullRuntime)

        val finalStatus = maybePullRuntime.get

        assert(finalStatus.name === "myname")
        assert(finalStatus.version === "myversion")
        assert(finalStatus.status === ServiceTaskStatus.Finished)
      }
    }
  }
}