package io.hydrosphere.serving.manager.service

import akka.testkit.TestProbe
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.service.actors.RepositoryIndexActor
import io.hydrosphere.serving.manager.test.FullIntegrationSpec
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._

class ApplicationServiceITSpec extends FullIntegrationSpec with BeforeAndAfterAll{

  "Application service" should {
    "create a simple application" in {
      for {
        version <- managerServices.modelManagementService.buildModel(1, None)
        appRequest = ApplicationCreateOrUpdateRequest(
          id = None,
          name = "testapp",
          executionGraph = ApplicationExecutionGraph(
            stages = List(
              ApplicationStage(
                services = List(
                  ServiceWeight(
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
          )
        )
        app <- managerServices.applicationManagementService.createApplication(appRequest)
      } yield {
        println(app)
        assert(app.name === appRequest.name)
        assert(app.contract === version.modelContract)
        assert(app.executionGraph === appRequest.executionGraph)
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
    indexProbe.expectMsg(15.seconds, RepositoryIndexActor.IndexFinished("dummy_model", "itsource"))
  }
}
