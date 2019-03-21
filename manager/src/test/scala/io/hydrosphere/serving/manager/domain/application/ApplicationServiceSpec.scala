package io.hydrosphere.serving.manager.domain.application

import java.time.LocalDateTime

import cats.effect.IO
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionRepository, ModelVersionStatus}
import io.hydrosphere.serving.manager.domain.servable.{Servable, ServableRepository, ServableService}
import io.hydrosphere.serving.manager.infrastructure.envoy.events.ApplicationDiscoveryEventBus
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
        val servableRepo = mock[ServableRepository[IO]]
        when(servableRepo.fetchByIds(Seq(1))).thenReturn(IO(Seq.empty))
        val servableService = new ServableService[IO] {
          override def delete(serviceId: Long): IO[Servable] = ???
          override def deleteServables(services: List[Long]): IO[List[Servable]] = ???
          override def create(servableName: String, configParams: Option[Map[String, String]], modelVersion: ModelVersion): IO[Servable] = ???
          override def deployModelVersions(modelVersions: Set[ModelVersion]): IO[List[Servable]] = {
            IO.pure(
              modelVersions.toList.zipWithIndex.map {
                case (v, id) =>
                  Servable(id, v.model.name + v.modelVersion, None, v, "", Map.empty)
              }
            )
          }
        }

        val eventPublisher = mock[ApplicationDiscoveryEventBus[IO]]
        val applicationService = ApplicationService[IO](
          appRepo,
          versionRepo,
          servableService,
          servableRepo,
          eventPublisher
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
          override def delete(serviceId: Long): IO[Servable] = ???

          override def deleteServables(services: List[Long]): IO[List[Servable]] = ???

          override def create(servableName: String, configParams: Option[Map[String, String]], modelVersion: ModelVersion): IO[Servable] = ???

          override def deployModelVersions(modelVersions: Set[ModelVersion]): IO[List[Servable]] = {
            IO.raiseError(new RuntimeException("Test error"))
          }
        }
        val servableRepo = mock[ServableRepository[IO]]
        when(servableRepo.fetchByIds(Seq(1))).thenReturn(IO(Seq.empty))

        val eventPublisher = mock[ApplicationDiscoveryEventBus[IO]]
        val applicationService = ApplicationService(
          appRepo,
          versionRepo,
          servableService,
          servableRepo,
          eventPublisher
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
          override def delete(serviceId: Long): IO[Servable] = ???
          override def deleteServables(services: List[Long]): IO[List[Servable]] = ???
          override def create(servableName: String, configParams: Option[Map[String, String]], modelVersion: ModelVersion): IO[Servable] = IO(Servable(
            id = 1,
            serviceName = servableName,
            cloudDriverId = None,
            modelVersion = modelVersion,
            statusText = "asd",
            configParams = configParams.getOrElse(Map.empty)
          ))
          override def deployModelVersions(modelVersions: Set[ModelVersion]): IO[List[Servable]] = {
            IO.pure(
              modelVersions.toList.zipWithIndex.map {
                case (v, id) =>
                  Servable(id, v.model.name + v.modelVersion, None, v, "", Map.empty)
              }
            )
          }
        }
        val servableRepo = mock[ServableRepository[IO]]
        when(servableRepo.fetchByIds(Seq(1))).thenReturn(IO(Seq.empty))

        val appChanged = ListBuffer.empty[Application]
        val eventPublisher = new ApplicationDiscoveryEventBus[IO] {
          override def detected(application: Application) = IO(appChanged += application)

          override def removed(application: Application) = ???

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
          servableRepo,
          eventPublisher
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
          override def delete(serviceId: Long): IO[Servable] = ???

          override def deleteServables(services: List[Long]): IO[List[Servable]] = IO.pure(List.empty)

          override def create(servableName: String, configParams: Option[Map[String, String]], modelVersion: ModelVersion): IO[Servable] = {
            IO(Servable(
              id = 1,
              serviceName = "name",
              cloudDriverId = None,
              modelVersion = modelVersion,
              statusText = "asd",
              configParams = Map.empty
            ))
          }
          override def deployModelVersions(modelVersions: Set[ModelVersion]): IO[List[Servable]] = {
            IO.pure(
              modelVersions.toList.zipWithIndex.map {
                case (v, id) =>
                  Servable(id, v.model.name + v.modelVersion, None, v, "", Map.empty)
              }
            )
          }
        }
        val servableRepo = mock[ServableRepository[IO]]
        when(servableRepo.fetchByIds(Matchers.any())).thenReturn(IO(Seq.empty))

        val appChanged = ListBuffer.empty[Application]
        val appDeleted = ListBuffer.empty[Application]
        val eventPublisher = new ApplicationDiscoveryEventBus[IO]  {
          override def detected(application: Application) = IO(appChanged += application)
          override def removed(application: Application) = IO(appDeleted += application)
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
          servableRepo,
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