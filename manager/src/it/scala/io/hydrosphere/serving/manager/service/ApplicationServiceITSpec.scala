package io.hydrosphere.serving.manager.service

import cats.data.EitherT
import cats.instances.all._
import io.hydrosphere.serving.manager.controller.application._
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.test.FullIntegrationSpec
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class ApplicationServiceITSpec extends FullIntegrationSpec with BeforeAndAfterAll {
  val upload1 = ModelUpload(
    packModel("/models/dummy_model"),
    name = Some("m1")
  )
  val upload2 = ModelUpload(
    packModel("/models/dummy_model_2"),
    name = Some("m2")
  )

  "Application service" should {
    "create a simple application" in {
      for {
        version <- managerServices.modelBuildManagmentService.buildModel(1, None)
        appRequest = CreateApplicationRequest(
          name = "testapp",
          executionGraph = ExecutionGraphRequest(
            stages = List(
              ExecutionStepRequest(
                services = List(
                  SimpleServiceDescription(
                    runtimeId = 1, // dummy runtime id
                    modelVersionId = Some(version.right.get.id),
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
        appResult <- managerServices.applicationManagementService.createApplication(
          appRequest.name,
          appRequest.executionGraph,
          appRequest.kafkaStreaming
        )
      } yield {
        println(appResult)
        val app = appResult.right.get
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
        assert(app.contract === version.right.get.modelContract)
        assert(app.executionGraph === expectedGraph)
      }
    }

    "create a multi-service stage" in {
      for {
        versionResult <- managerServices.modelBuildManagmentService.buildModel(1, None)
        version = versionResult.right.get
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
        appRes <- managerServices.applicationManagementService.createApplication(
          appRequest.name,
          appRequest.executionGraph,
          appRequest.kafkaStreaming
        )
      } yield {
        assert(appRes.isRight, appRes)
        val app = appRes.right.get
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
        version <- managerServices.modelBuildManagmentService.buildModel(1, None)
        appRequest = CreateApplicationRequest(
          name = "kafka_app",
          executionGraph = ExecutionGraphRequest(
            stages = List(
              ExecutionStepRequest(
                services = List(
                  SimpleServiceDescription(
                    runtimeId = 1, // dummy runtime id
                    modelVersionId = Some(version.right.get.id),
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
        appRes <- managerServices.applicationManagementService.createApplication(
          appRequest.name,
          appRequest.executionGraph,
          appRequest.kafkaStreaming
        )
        app = appRes.right.get

        appResNew <- managerServices.applicationManagementService.updateApplication(
          app.id,
          app.name,
          appRequest.executionGraph,
          Seq.empty
        )
        appNew = appResNew.right.get

        maybeGotNewApp <- managerServices.applicationManagementService.getApplication(appNew.id)
      } yield {
        println(app)
        assert(maybeGotNewApp.isRight, s"Couldn't find updated application in repository ${appNew}")
        assert(appNew === maybeGotNewApp.right.get)
        assert(appNew.kafkaStreaming.isEmpty, appNew)
      }
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()

    dockerClient.pull("hydrosphere/serving-runtime-dummy:latest")

    val f = for {
      d1 <- EitherT(managerServices.modelManagementService.uploadModel(upload1))
      d2 <- EitherT(managerServices.modelManagementService.uploadModel(upload2))
    } yield {
      println(s"UPLOADED: $d1")
      d2
    }

    Await.result(f.value, 30 seconds)
  }
}
