package io.hydrosphere.serving.manager.it.service

import cats.data.EitherT
import cats.instances.all._
import io.hydrosphere.serving.manager.controller.application._
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.it.FullIntegrationSpec
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.service.environment.AnyEnvironment
import io.hydrosphere.serving.manager.service.model_build.BuildModelRequest
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class ApplicationServiceITSpec extends FullIntegrationSpec with BeforeAndAfterAll {
  implicit val awaitTimeout = 50.seconds
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
      eitherTAssert {
        for {
          runtime <- EitherT(managerServices.runtimeManagementService.get(1))
          modelBuild <- EitherT(managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(1)))
          modelVersion <- EitherT.liftF(modelBuild.future)
          appRequest = CreateApplicationRequest(
            name = "testapp",
            namespace = None,
            executionGraph = ExecutionGraphRequest(
              stages = List(
                ExecutionStepRequest(
                  services = List(
                    ServiceCreationDescription(
                      runtimeId = runtime.id, // dummy runtime id
                      modelVersionId = Some(modelVersion.id),
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
          appResult <- EitherT(managerServices.applicationManagementService.createApplication(
            appRequest.name,
            appRequest.namespace,
            appRequest.executionGraph,
            appRequest.kafkaStreaming
          ))
        } yield {
          println(appResult)
          val expectedGraph = ApplicationExecutionGraph(
            List(
              ApplicationStage(
                List(
                  DetailedServiceDescription(
                    weight = 100,
                    signature = None,
                    runtime = runtime,
                    modelVersion = modelVersion,
                    environment = AnyEnvironment
                  )
                ),
                None,
                Map.empty
              )
            )
          )
          assert(appResult.name === appRequest.name)
          assert(appResult.contract === modelVersion.modelContract)
          assert(appResult.executionGraph === expectedGraph)
        }
      }
    }

    "create a multi-service stage" in {
      eitherTAssert {
        for {
          runtime <- EitherT(managerServices.runtimeManagementService.get(1))
          modelBuild <- EitherT(managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(1)))
          modelVersion <- EitherT.liftF(modelBuild.future)
          appRequest = CreateApplicationRequest(
            name = "MultiServiceStage",
            namespace = None,
            executionGraph = ExecutionGraphRequest(
              stages = List(
                ExecutionStepRequest(
                  services = List(
                    ServiceCreationDescription(
                      runtimeId = runtime.id,
                      modelVersionId = Some(modelVersion.id),
                      environmentId = None,
                      weight = 50,
                      signatureName = "default_spark"
                    ),
                    ServiceCreationDescription(
                      runtimeId = runtime.id,
                      modelVersionId = Some(modelVersion.id),
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
          app <- EitherT(managerServices.applicationManagementService.createApplication(
            appRequest.name,
            appRequest.namespace,
            appRequest.executionGraph,
            appRequest.kafkaStreaming
          ))
        } yield {
          println(app)
          val expectedGraph = ApplicationExecutionGraph(
            List(
              ApplicationStage(
                List(
                  DetailedServiceDescription(
                    weight = 50,
                    signature = modelVersion.modelContract.signatures.find(_.signatureName == "default_spark"),
                    runtime = runtime,
                    modelVersion = modelVersion,
                    environment = AnyEnvironment
                  ),
                  DetailedServiceDescription(
                    weight = 50,
                    signature = modelVersion.modelContract.signatures.find(_.signatureName == "default_spark"),
                    runtime = runtime,
                    modelVersion = modelVersion,
                    environment = AnyEnvironment
                  )
                ),
                modelVersion.modelContract.signatures.find(_.signatureName == "default_spark").map(_.withSignatureName("0")),
                dataProfileFields = Map.empty
              )
            )
          )
          assert(app.name === appRequest.name)
          assert(app.executionGraph === expectedGraph)
        }
      }
    }

    "create and update an application with kafkaStreaming" in {
      eitherTAssert {
        for {
          modelBuild <- EitherT(managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(1)))
          modelVersion <- EitherT.liftF(modelBuild.future)
          appRequest = CreateApplicationRequest(
            name = "kafka_app",
            namespace = None,
            executionGraph = ExecutionGraphRequest(
              stages = List(
                ExecutionStepRequest(
                  services = List(
                    ServiceCreationDescription(
                      runtimeId = 1, // dummy runtime id
                      modelVersionId = Some(modelVersion.id),
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
          app <- EitherT(managerServices.applicationManagementService.createApplication(
            appRequest.name,
            appRequest.namespace,
            appRequest.executionGraph,
            appRequest.kafkaStreaming
          ))
          appNew <- EitherT(managerServices.applicationManagementService.updateApplication(
            app.id,
            app.name,
            app.namespace,
            appRequest.executionGraph,
            Seq.empty
          ))

          gotNewApp <- EitherT(managerServices.applicationManagementService.getApplication(appNew.id))
        } yield {
          assert(appNew === gotNewApp)
          assert(appNew.kafkaStreaming.isEmpty, appNew)
        }
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
