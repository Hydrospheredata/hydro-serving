package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.controller.application._
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.LocalSourceParams
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.test.FullIntegrationSpec
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
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
                  SimpleServiceDescription(
                    runtimeId = 1, // dummy runtime id
                    modelVersionId = Some(version.id),
                    environmentId = None,
                    weight = 0,
                    signatureName = "default"
                  )
                )
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
                    runtimeId = 1,
                    modelVersionId = Some(1),
                    environmentId = None
                  ),
                  weight = 100,
                  signature = None
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

    "create a multi-service stage" in {
      for {
        version <- managerServices.modelManagementService.buildModel(1, None)
        appRequest = CreateApplicationRequest(
          name = "MultiServiceStage",
          executionGraph = ExecutionGraphRequest(
            stages = List(
              ExecutionStepRequest(
                services = List(
                  SimpleServiceDescription(
                    runtimeId = 1, // dummy runtime id
                    modelVersionId = Some(version.id),
                    environmentId = None,
                    weight = 50,
                    signatureName = "default_spark"
                  ),
                  SimpleServiceDescription(
                    runtimeId = 1, // dummy runtime id
                    modelVersionId = Some(version.id),
                    environmentId = None,
                    weight = 50,
                    signatureName = "default_spark"
                  )
                )
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
                    runtimeId = 1,
                    modelVersionId = Some(version.id),
                    environmentId = None
                  ),
                  weight = 50,
                  signature = version.modelContract.signatures.find(_.signatureName == "default_spark")
                ),
                WeightedService(
                  ServiceKeyDescription(
                    runtimeId = 1,
                    modelVersionId = Some(version.id),
                    environmentId = None
                  ),
                  weight = 50,
                  signature = version.modelContract.signatures.find(_.signatureName == "default_spark")
                )
              ),
              version.modelContract.signatures.find(_.signatureName == "default_spark").map(_.withSignatureName("0"))
            )
          )
        )
        assert(app.name === appRequest.name)
        assert(app.executionGraph === expectedGraph)
      }
    }

    "create and update an application with kafkaStreaming" in {
      for {
        version <- managerServices.modelManagementService.buildModel(1, None)
        appRequest = CreateApplicationRequest(
          name = "kafka_app",
          executionGraph = ExecutionGraphRequest(
            stages = List(
              ExecutionStepRequest(
                services = List(
                  SimpleServiceDescription(
                    runtimeId = 1, // dummy runtime id
                    modelVersionId = Some(version.id),
                    environmentId = None,
                    weight = 100,
                    signatureName = "default"
                  )
                )
              )
            )
          ),
          kafkaStreaming = List(
            ApplicationKafkaStream(
              sourceTopic = "source",
              destinationTopic = "dest",
              consumerId = None,
              errorTopic = None
            )
          )
        )
        app <- managerServices.applicationManagementService.createApplication(appRequest)
        appUpdate = UpdateApplicationRequest(
          id = app.id,
          name = app.name,
          executionGraph = appRequest.executionGraph,
          kafkaStream = None
        )
        appNew <- managerServices.applicationManagementService.updateApplication(appUpdate)
        maybeGotNewApp <- managerServices.applicationManagementService.getApplication(appNew.id)
      } yield {
        assert(maybeGotNewApp.isDefined, "Couldn't find updated application in repository")
        assert(appNew === maybeGotNewApp.get)
        assert(appNew.kafkaStreaming.isEmpty, appNew)
      }
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    dockerClient.pull("hydrosphere/serving-runtime-dummy:latest")
    val sourceConf = ModelSourceConfig(1, "itsource", LocalSourceParams(Some(getClass.getResource("/models").getPath)))
    val f = for {
      _ <- managerServices.sourceManagementService.addSource(sourceConf)
      m <- managerServices.modelManagementService.addModel("itsource", "dummy_model")
    } yield m

    Await.result(f, 30 seconds)
  }
}
