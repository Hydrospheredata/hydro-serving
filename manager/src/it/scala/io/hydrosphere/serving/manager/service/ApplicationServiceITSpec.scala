package io.hydrosphere.serving.manager.service

import akka.testkit.TestProbe
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.controller.application.{CreateApplicationRequest, ExecutionGraphRequest, ExecutionStepRequest}
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.service.actors.RepositoryIndexActor
import io.hydrosphere.serving.manager.test.FullIntegrationSpec
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._

class ApplicationServiceITSpec extends FullIntegrationSpec with BeforeAndAfterAll {

  "Application service" should {
    "create a simple application" in {
      for {
        version <- managerServices.modelManagementService.buildModel(1, None)
        appRequest = CreateApplicationRequest(
          name = "testapp",
          executionGraph = ExecutionGraphRequest(
            stages = List(
              ExecutionStepRequest(
                services = List(
                  WeightedService(
                    serviceDescription = ServiceKeyDescription(
                      runtimeId = 1, // dummy runtime id
                      modelVersionId = Some(version.id),
                      environmentId = None
                    ),
                    weight = 100
                  )
                ),
                signatureName = "default"
              )
            )
          ),
          kafkaStreaming = List.empty
        )
        app <- managerServices.applicationManagementService.createApplication(appRequest)
      } yield {
        println(app)
        val expectedGraph = ApplicationExecutionGraph(
          List(
            ApplicationStage(
              List(
                WeightedService(
                  ServiceKeyDescription(
                    1,
                    Some(1),
                    None
                  ),
                  100
                )
              ),
              None
            )
          )
        )
        assert(app.name === appRequest.name)
        assert(app.contract === version.modelContract)
        assert(app.executionGraph === expectedGraph)
      }
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    dockerClient.pull("hydrosphere/serving-runtime-dummy:latest")
    val indexProbe = TestProbe()
    system.eventStream.subscribe(indexProbe.ref, classOf[RepositoryIndexActor.IndexFinished])
    managerServices.sourceManagementService.addSource(
      ModelSourceConfig(1, "itsource", LocalSourceParams(getClass.getResource("/models").getPath)).toAux
    )
    indexProbe.expectMsgAllOf(
      20.seconds,
      RepositoryIndexActor.IndexFinished("dummy_model", "itsource"),
      RepositoryIndexActor.IndexFinished("dummy_model_2", "itsource")
    )
  }
}
