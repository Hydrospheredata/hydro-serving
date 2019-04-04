package io.hydrosphere.serving.manager.domain.application

import java.time.LocalDateTime

import cats.effect.IO
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.discovery.DiscoveryEvent.{AppRemoved, AppStarted}
import io.hydrosphere.serving.manager.discovery.{DiscoveryEvent, DiscoveryHub}
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionRepository, ModelVersionStatus}
import io.hydrosphere.serving.manager.domain.servable.{Servable, ServableService, ServableStatus}
import io.hydrosphere.serving.manager.grpc.entities.ServingApp
import io.hydrosphere.serving.tensorflow.types.DataType
import org.mockito.Matchers

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

class ApplicationServiceSpec extends GenericUnitTest {

  val signature = ModelSignature(
    "claim",
    Seq(ModelField("in", None, typeOrSubfields = ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE))),
    Seq(ModelField("out", None, typeOrSubfields = ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE)))
  )
  val contract = ModelContract("", Some(signature))
  val modelVersion = ModelVersion(
    id = 1,
    image = DockerImage("test", "t"),
    created = LocalDateTime.now(),
    finished = None,
    modelVersion = 1,
    modelContract = contract,
    runtime = DockerImage("runtime", "v"),
    model = Model(1, "model"),
    hostSelector = None,
    status = ModelVersionStatus.Released,
    profileTypes = Map.empty,
    installCommand = None,
    metadata = Map.empty
  )

  describe("Application management service") {
    implicit val cs = IO.contextShift(ExecutionContext.global)
    it("should start application build") {
      ioAssert {
        val appRepo = mock[ApplicationRepository[IO]]
        when(appRepo.get("test")).thenReturn(IO(None))
        when(appRepo.create(Matchers.any())).thenReturn(IO(
          Application(
            id = 1,
            name = "test",
            namespace = None,
            status = ApplicationStatus.Assembling,
            signature = signature.copy(signatureName = "test"),
            executionGraph = ApplicationExecutionGraph(Seq(
              PipelineStage(Seq(
                ModelVariant(modelVersion, 100)
              ), signature)
            )),
            kafkaStreaming = List.empty
          )
        ))
        val versionRepo = mock[ModelVersionRepository[IO]]
        when(versionRepo.get(1)).thenReturn(IO(Some(modelVersion)))
        when(versionRepo.get(Seq(1L))).thenReturn(IO(Seq(modelVersion)))
        val servableService = new ServableService[IO] {
          override def deploy(name: String, modelVersionId: Long, image: DockerImage): IO[Servable] = {
            IO.pure(
              Servable(1, name + modelVersionId.toString, ServableStatus.Starting)
            )
          }

          override def stop(name: String): IO[Unit] = ???
        }

        val discoveryHub = mock[DiscoveryHub[IO]]
        val applicationService = ApplicationService[IO](
          appRepo,
          versionRepo,
          servableService,
          discoveryHub
        )
        val graph = ExecutionGraphRequest(List(
          PipelineStageRequest(Seq(
            ModelVariantRequest(
              modelVersionId = 1,
              weight = 100
            )
          ))
        ))
        val createReq = CreateApplicationRequest("test", None, graph, Option.empty)
        applicationService.create(createReq).map { res =>
          assert(res.isRight, res)
          val app = res.right.get
          assert(app.started.name === "test")
          assert(app.started.status === ApplicationStatus.Assembling)
          // build will fail nonetheless
        }
      }
    }

    it("should handle failed application builds") {
      ioAssert {
        val appRepo = mock[ApplicationRepository[IO]]
        when(appRepo.update(Matchers.any())).thenReturn(IO.pure(1))
        when(appRepo.get("test")).thenReturn(IO(None))
        when(appRepo.create(Matchers.any())).thenReturn(IO(
          Application(
            id = 1,
            name = "test",
            namespace = None,
            status = ApplicationStatus.Assembling,
            signature = signature.copy(signatureName = "test"),
            executionGraph = ApplicationExecutionGraph(Seq(
              PipelineStage(Seq(
                ModelVariant(modelVersion, 100)
              ), signature)
            )),
            kafkaStreaming = List.empty
          )
        ))
        val versionRepo = mock[ModelVersionRepository[IO]]
        when(versionRepo.get(1)).thenReturn(IO(Some(modelVersion)))
        when(versionRepo.get(Seq(1L))).thenReturn(IO(Seq(modelVersion)))
        val servableService = new ServableService[IO] {
          override def stop(name: String): IO[Unit] = ???

          override def deploy(name: String, modelVersionId: Long, image: DockerImage): IO[Servable] = {
            IO.raiseError(new RuntimeException("Test error"))
          }
        }

        val discoveryHub = mock[DiscoveryHub[IO]]
        val applicationService = ApplicationService(
          appRepo,
          versionRepo,
          servableService,
          discoveryHub
        )
        val graph = ExecutionGraphRequest(List(
          PipelineStageRequest(Seq(
            ModelVariantRequest(
              modelVersionId = 1,
              weight = 100
            )
          ))
        ))
        val createReq = CreateApplicationRequest("test", None, graph, Option.empty)
        applicationService.create(createReq).flatMap { res =>
          assert(res.isRight, res)
          val app = res.right.get
          app.completed.get.map { x =>
            assert(x.status === ApplicationStatus.Failed)
          }
        }
      }
    }

    it("should handle finished builds") {
      ioAssert {
        val appRepo = mock[ApplicationRepository[IO]]
        when(appRepo.update(Matchers.any())).thenReturn(IO(1))
        when(appRepo.get("test")).thenReturn(IO(None))
        when(appRepo.create(Matchers.any())).thenReturn(IO(
          Application(
            id = 1,
            name = "test",
            namespace = None,
            status = ApplicationStatus.Assembling,
            signature = signature.copy(signatureName = "test"),
            executionGraph = ApplicationExecutionGraph(Seq(
              PipelineStage(Seq(
                ModelVariant(modelVersion, 100)
              ), signature)
            )),
            kafkaStreaming = List.empty
          )
        ))
        val versionRepo = mock[ModelVersionRepository[IO]]
        when(versionRepo.get(1)).thenReturn(IO(Some(modelVersion)))
        when(versionRepo.get(Seq(1L))).thenReturn(IO(Seq(modelVersion)))
        val servableService = new ServableService[IO] {
          override def stop(name: String): IO[Unit] = ???

          override def deploy(name: String, modelVersionId: Long, image: DockerImage): IO[Servable] = IO.pure {
            Servable(
              modelVersionId = modelVersionId,
              serviceName = name + modelVersionId,
              status = ServableStatus.Running("imaginaryhost", 6969)
            )
          }
        }

        val appChanged = ListBuffer.empty[ServingApp]
        val discoveryHub = new DiscoveryHub[IO] {
          override def update(e: DiscoveryEvent): IO[Unit] = e match {
            case AppStarted(app) => IO(appChanged += app)
            case _ => IO.unit
          }

          override def current: IO[List[ServingApp]] = IO.pure(appChanged.toList)
        }
        val graph = ExecutionGraphRequest(List(
          PipelineStageRequest(Seq(
            ModelVariantRequest(
              modelVersionId = 1,
              weight = 100
            )
          ))
        ))
        val applicationService = ApplicationService[IO](
          appRepo,
          versionRepo,
          servableService,
          discoveryHub
        )
        val createReq = CreateApplicationRequest("test", None, graph, Option.empty)
        applicationService.create(createReq).flatMap { res =>
          val app = res.right.get
          app.completed.get.map { finished =>
            assert(finished.name === "test")
            assert(finished.status === ApplicationStatus.Ready)
            assert(appChanged.nonEmpty)
          }
        }
      }
    }

    it("should rebuild on update") {
      ioAssert {
        val appRepo = mock[ApplicationRepository[IO]]
        when(appRepo.get(Matchers.any[Long]())).thenReturn(IO(Some(
          Application(
            id = 1,
            name = "test",
            namespace = None,
            status = ApplicationStatus.Assembling,
            signature = signature.copy(signatureName = "test"),
            executionGraph = ApplicationExecutionGraph(Seq(
              PipelineStage(Seq(
                ModelVariant(modelVersion, 100)
              ), signature)
            )),
            kafkaStreaming = List.empty
          )
        )))
        when(appRepo.get(Matchers.any[String]())).thenReturn(IO(None))
        when(appRepo.create(Matchers.any())).thenReturn(IO(
          Application(
            id = 1,
            name = "test",
            namespace = None,
            status = ApplicationStatus.Assembling,
            signature = signature.copy(signatureName = "test"),
            executionGraph = ApplicationExecutionGraph(Seq(
              PipelineStage(Seq(
                ModelVariant(modelVersion, 100)
              ), signature)
            )),
            kafkaStreaming = List.empty
          )
        ))
        when(appRepo.applicationsWithCommonServices(Matchers.any(), Matchers.any())).thenReturn(IO(Seq.empty))
        when(appRepo.update(Matchers.any())).thenReturn(IO(1))

        val versionRepo = mock[ModelVersionRepository[IO]]
        when(versionRepo.get(Matchers.any[Long]())).thenReturn(IO(Some(modelVersion)))
        when(versionRepo.get(Matchers.any[Seq[Long]]())).thenReturn(IO(Seq(modelVersion)))

        val servableService = new ServableService[IO] {
          override def deploy(name: String, modelVersionId: Long, image: DockerImage): IO[Servable] = {
            IO.pure(
              Servable(1, name + modelVersionId.toString, ServableStatus.Starting)
            )
          }

          override def stop(name: String): IO[Unit] = ???
        }

        val apps = ListBuffer.empty[ServingApp]
        val eventPublisher = new DiscoveryHub[IO] {
          override def update(e: DiscoveryEvent): IO[Unit] = e match {
            case AppStarted(app) => IO(apps += app)
            case AppRemoved(id) => IO(apps --= apps.filter(_.id == id.toString))
          }

          override def current: IO[List[ServingApp]] = IO.pure(apps.toList)
        }
        val graph = ExecutionGraphRequest(List(
          PipelineStageRequest(Seq(
            ModelVariantRequest(
              modelVersionId = 1,
              weight = 100
            )
          ))
        ))
        val applicationService = ApplicationService[IO](
          appRepo,
          versionRepo,
          servableService,
          eventPublisher
        )
        val updateReq = UpdateApplicationRequest(1, "test", None, graph, Option.empty)
        applicationService.update(updateReq).map { res =>
          val app = res.right.get
          assert(app.started.name === "test")
          assert(app.started.status === ApplicationStatus.Assembling)
        }
      }
    }
  }
}