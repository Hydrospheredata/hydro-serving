package io.hydrosphere.serving.manager.domain.application

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.domain.clouddriver.CloudService
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionRepository, ModelVersionStatus}
import io.hydrosphere.serving.manager.domain.servable.{Servable, ServableService}
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.ManagerEventBus
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.tensorflow.types.DataType
import org.mockito.Matchers

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class ApplicationServiceSpec extends GenericUnitTest {

  val signature = ModelSignature(
    "claim",
    Seq(ModelField("in", None, typeOrSubfields = ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE))),
    Seq(ModelField("out", None, typeOrSubfields = ModelField.TypeOrSubfields.Dtype(DataType.DT_DOUBLE)))
  )
  val contract = ModelContract("model",Seq(signature))
  val modelVersion = ModelVersion(
    id = 1,
    image = DockerImage("test", "t"),
    created = LocalDateTime.now(),
    finished = None,
    modelVersion = 1,
    modelType = ModelType.Unknown(),
    modelContract = contract,
    runtime = DockerImage("runtime", "v"),
    model = Model(1, "model"),
    hostSelector = None,
    status = ModelVersionStatus.Released,
    profileTypes = Map.empty
  )

  describe("Application management service") {
    it("should start application build") {
      val appRepo = mock[ApplicationRepository[Future]]
      when(appRepo.get("test")).thenReturn(Future.successful(None))
      when(appRepo.create(Matchers.any())).thenReturn(Future.successful(
        Application(
          id = 1,
          name = "test",
          namespace = None,
          status = ApplicationStatus.Assembling,
          signature = signature.copy(signatureName = "test"),
          executionGraph = ApplicationExecutionGraph(Seq(
            PipelineStage(Seq(
              ModelVariant(modelVersion, 100, signature)
            ), signature)
          )),
          kafkaStreaming = List.empty
        )
      ))
      val versionRepo = mock[ModelVersionRepository[Future]]
      when(versionRepo.get(1)).thenReturn(Future.successful(Some(modelVersion)))
      when(versionRepo.get(Seq(1L))).thenReturn(Future.successful(Seq(modelVersion)))
      val serviceManager = mock[ServableService]
      when(serviceManager.fetchServicesUnsync(Set(1))).thenReturn(Future.successful(Seq.empty))
      val eventPublisher = mock[ManagerEventBus]
      val applicationService = new ApplicationService(
        appRepo,
        versionRepo,
        serviceManager,
        eventPublisher
      )
      val graph = ExecutionGraphRequest(Seq(
        PipelineStageRequest(Seq(
          ModelVariantRequest(
            modelVersionId = 1,
            weight = 100,
            signatureName = "claim"
          )
        ))
      ))
      applicationService.create("test", None, graph, Seq.empty).map { res =>
        assert(res.isRight, res)
        val app = res.right.get
        assert(app.started.name === "test")
        assert(app.started.status === ApplicationStatus.Assembling)
        // build will fail nonetheless
      }
    }

    it("should handle failed application builds") {
      val appRepo = mock[ApplicationRepository[Future]]
      when(appRepo.get("test")).thenReturn(Future.successful(None))
      when(appRepo.create(Matchers.any())).thenReturn(Future.successful(
        Application(
          id = 1,
          name = "test",
          namespace = None,
          status = ApplicationStatus.Assembling,
          signature = signature.copy(signatureName = "test"),
          executionGraph = ApplicationExecutionGraph(Seq(
            PipelineStage(Seq(
              ModelVariant(modelVersion, 100, signature)
            ), signature)
          )),
          kafkaStreaming = List.empty
        )
      ))
      val versionRepo = mock[ModelVersionRepository[Future]]
      when(versionRepo.get(1)).thenReturn(Future.successful(Some(modelVersion)))
      when(versionRepo.get(Seq(1L))).thenReturn(Future.successful(Seq(modelVersion)))
      val serviceManager = mock[ServableService]
      when(serviceManager.addServable(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.failed(new RuntimeException("Test error")))
      when(serviceManager.fetchServicesUnsync(Set(1))).thenReturn(Future.successful(Seq.empty))

      val appChanged = ListBuffer.empty[Application]
      val appDeleted = ListBuffer.empty[Application]
      val eventPublisher = new ManagerEventBus {
        override def applicationChanged(application: Application): Unit = appChanged += application
        override def applicationRemoved(application: Application): Unit = appDeleted += application
        override def serviceChanged(service: Servable): Unit = ???
        override def serviceRemoved(service: Servable): Unit = ???
        override def cloudServiceDetected(cloudService: Seq[CloudService]): Unit = ???
        override def cloudServiceRemoved(cloudService: Seq[CloudService]): Unit = ???
      }
      val applicationService = new ApplicationService(
        appRepo,
        versionRepo,
        serviceManager,
        eventPublisher
      )
      val graph = ExecutionGraphRequest(Seq(
        PipelineStageRequest(Seq(
          ModelVariantRequest(
            modelVersionId = 1,
            weight = 100,
            signatureName = "claim"
          )
        ))
      ))
      applicationService.create("test", None, graph, Seq.empty).flatMap{ res =>
        assert(res.isRight, res)
        val app = res.right.get
        app.completed.foreach { x =>
          println(x)
          fail("Future should fail")
        }
        app.completed.failed.map{ _ =>
          assert(appChanged.isEmpty)
          assert(appDeleted.isEmpty)
        }
      }
    }

    it("should handle finished builds") {
      val appRepo = mock[ApplicationRepository[Future]]
      when(appRepo.get("test")).thenReturn(Future.successful(None))
      when(appRepo.create(Matchers.any())).thenReturn(Future.successful(
        Application(
          id = 1,
          name = "test",
          namespace = None,
          status = ApplicationStatus.Assembling,
          signature = signature.copy(signatureName = "test"),
          executionGraph = ApplicationExecutionGraph(Seq(
            PipelineStage(Seq(
              ModelVariant(modelVersion, 100, signature)
            ), signature)
          )),
          kafkaStreaming = List.empty
        )
      ))
      val versionRepo = mock[ModelVersionRepository[Future]]
      when(versionRepo.get(1)).thenReturn(Future.successful(Some(modelVersion)))
      when(versionRepo.get(Seq(1L))).thenReturn(Future.successful(Seq(modelVersion)))
      val serviceManager = mock[ServableService]
      when(serviceManager.addServable(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
        Future(Right(Servable(
          id = 1,
          serviceName = "name",
          cloudDriverId = None,
          modelVersion = modelVersion,
          statusText = "asd",
          configParams = Map.empty
        )))
      )
      when(serviceManager.fetchServicesUnsync(Set(1))).thenReturn(Future.successful(Seq.empty))

      val appChanged = ListBuffer.empty[Application]
      val appDeleted = ListBuffer.empty[Application]
      val eventPublisher = new ManagerEventBus {
        override def applicationChanged(application: Application): Unit = appChanged += application
        override def applicationRemoved(application: Application): Unit = appDeleted += application
        override def serviceChanged(service: Servable): Unit = ???
        override def serviceRemoved(service: Servable): Unit = ???
        override def cloudServiceDetected(cloudService: Seq[CloudService]): Unit = ???
        override def cloudServiceRemoved(cloudService: Seq[CloudService]): Unit = ???
      }
      val graph = ExecutionGraphRequest(Seq(
        PipelineStageRequest(Seq(
          ModelVariantRequest(
            modelVersionId = 1,
            weight = 100,
            signatureName = "claim"
          )
        ))
      ))
      val applicationService = new ApplicationService(
        appRepo,
        versionRepo,
        serviceManager,
        eventPublisher
      )
      applicationService.create("test", None, graph, Seq.empty).flatMap { res =>
        val app = res.right.get
        app.completed.map { finished =>
          assert(finished.name === "test")
          assert(finished.status === ApplicationStatus.Ready)
          assert(appChanged.nonEmpty)
          assert(appDeleted.isEmpty)
        }
      }
    }
    it("should rebuild on update") {
      val appRepo = mock[ApplicationRepository[Future]]
      when(appRepo.get(1)).thenReturn(Future.successful(Some(
        Application(
          id = 1,
          name = "test",
          namespace = None,
          status = ApplicationStatus.Assembling,
          signature = signature.copy(signatureName = "test"),
          executionGraph = ApplicationExecutionGraph(Seq(
            PipelineStage(Seq(
              ModelVariant(modelVersion, 100, signature)
            ), signature)
          )),
          kafkaStreaming = List.empty
        )
      )))
      when(appRepo.get("test")).thenReturn(Future.successful(None))
      when(appRepo.create(Matchers.any())).thenReturn(Future.successful(
        Application(
          id = 1,
          name = "test",
          namespace = None,
          status = ApplicationStatus.Assembling,
          signature = signature.copy(signatureName = "test"),
          executionGraph = ApplicationExecutionGraph(Seq(
            PipelineStage(Seq(
              ModelVariant(modelVersion, 100, signature)
            ), signature)
          )),
          kafkaStreaming = List.empty
        )
      ))
      when(appRepo.applicationsWithCommonServices(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Seq.empty))
     when(appRepo.update(Matchers.any())).thenReturn(Future.successful(1))
      val versionRepo = mock[ModelVersionRepository[Future]]
      when(versionRepo.get(1)).thenReturn(Future.successful(Some(modelVersion)))
      when(versionRepo.get(Matchers.any[Seq[Long]]())).thenReturn(Future.successful(Seq(modelVersion)))
      val serviceManager = mock[ServableService]
      when(serviceManager.addServable(Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(
        Future(Right(Servable(
          id = 1,
          serviceName = "name",
          cloudDriverId = None,
          modelVersion = modelVersion,
          statusText = "asd",
          configParams = Map.empty
        )))
      )
      when(serviceManager.fetchServicesUnsync(Matchers.any())).thenReturn(Future.successful(Seq.empty))

      val appChanged = ListBuffer.empty[Application]
      val appDeleted = ListBuffer.empty[Application]
      val eventPublisher = new ManagerEventBus {
        override def applicationChanged(application: Application): Unit = appChanged += application
        override def applicationRemoved(application: Application): Unit = appDeleted += application
        override def serviceChanged(service: Servable): Unit = ???
        override def serviceRemoved(service: Servable): Unit = ???
        override def cloudServiceDetected(cloudService: Seq[CloudService]): Unit = ???
        override def cloudServiceRemoved(cloudService: Seq[CloudService]): Unit = ???
      }
      val graph = ExecutionGraphRequest(Seq(
        PipelineStageRequest(Seq(
          ModelVariantRequest(
            modelVersionId = 1,
            weight = 100,
            signatureName = "claim"
          )
        ))
      ))
      val applicationService = new ApplicationService(
        appRepo,
        versionRepo,
        serviceManager,
        eventPublisher
      )
      applicationService.update(1, "test", None, graph, Seq.empty).map{ res =>
        val app = res.right.get
        assert(app.started.name === "test")
        assert(app.started.status === ApplicationStatus.Assembling)
      }
    }
  }
}