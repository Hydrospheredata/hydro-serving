package io.hydrosphere.serving.manager.it.service

import cats.data.EitherT
import cats.instances.all._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.api.http.controller.application._
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.domain.application.{ApplicationExecutionGraph, ApplicationKafkaStream, PipelineStage, PipelineStageNode}
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.manager.it.FullIntegrationSpec
import io.hydrosphere.serving.model.api.Result.HError
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class ApplicationServiceITSpec extends FullIntegrationSpec with BeforeAndAfterAll {
  val uploadFile = packModel("/models/dummy_model")
  val upload1 = ModelUploadMetadata(
    name = Some("m1"),
    runtime = dummyImage,
  )
  val upload2 = ModelUploadMetadata(
    name = Some("m2"),
    runtime = dummyImage,
  )
  val upload3 = ModelUploadMetadata(
    name = Some("m1"),
    runtime = dummyImage,
    contract = Some(ModelContract(
      modelName = "m1",
      signatures = List(
        ModelSignature(
          signatureName = "not-default-spark",
          inputs = List(ModelField("test-input")),
          outputs = List(ModelField("test-output"))
        )
      )
    ))
  )

  var mv1: ModelVersion = _
  var mv2: ModelVersion = _

  "Application service" should {
    "create a simple application" in {
      eitherTAssert {
        for {
          appResult <- EitherT(managerServices.applicationManagementService.createApplication(
            "simple-app",
            None,
            ExecutionGraphRequest(Seq(
              ExecutionStepRequest(
                Seq(ServiceCreationDescription(
                  modelVersionId = mv1.id,
                  weight = 100,
                  signatureName = "any"
                ))
              ))
            ),
            Seq.empty
          ))
        } yield {
          println(appResult)
          val expectedStages = List(PipelineStageNode(
            weight = 100,
            signature = None,
            modelVersion = mv1,
          ))
          assert(appResult.name === "simple-app")
          assert(appResult.contract === mv1.modelContract)
          val services = appResult.executionGraph.stages.flatMap(_.services)
          val service = services.head
          assert(service.weight === 100)
          assert(service.signature === None)
          assert(service.modelVersion.id === mv1.id)
        }
      }
    }

    "create a multi-service stage" in {
      eitherTAssert {
        val appRequest = CreateApplicationRequest(
          name = "MultiServiceStage",
          namespace = None,
          executionGraph = ExecutionGraphRequest(
            stages = List(
              ExecutionStepRequest(
                modelVariants = List(
                  ServiceCreationDescription(
                    modelVersionId = mv1.id,
                    weight = 50,
                    signatureName = "default_spark"
                  ),
                  ServiceCreationDescription(
                    modelVersionId = mv1.id,
                    weight = 50,
                    signatureName = "default_spark"
                  )
                )
              )
            )
          ),
          kafkaStreaming = None
        )
        val expectedGraph = ApplicationExecutionGraph(
          List(
            PipelineStage(
              List(
                PipelineStageNode(
                  weight = 50,
                  signature = mv1.modelContract.signatures.find(_.signatureName == "default_spark"),
                  modelVersion = mv1,
                ),
                PipelineStageNode(
                  weight = 50,
                  signature = mv1.modelContract.signatures.find(_.signatureName == "default_spark"),
                  modelVersion = mv1,
                )
              ),
              mv1.modelContract.signatures.find(_.signatureName == "default_spark")
            )
          )
        )
        for {
          app <- EitherT(managerServices.applicationManagementService.createApplication(
            appRequest.name,
            appRequest.namespace,
            appRequest.executionGraph,
            Seq.empty
          ))
        } yield {
          println(app)
          assert(app.name === appRequest.name)
          val services = app.executionGraph.stages.flatMap(_.services)
          val service1 = services.head
          val service2 = services.head
          assert(service1.weight === 50)
          assert(service1.signature.exists(_.signatureName == "default_spark"))
          assert(service1.modelVersion.id === mv1.id)
          assert(service2.weight === 50)
          assert(service2.signature.exists(_.signatureName == "default_spark"))
          assert(service2.modelVersion.id === mv1.id)
        }
      }
    }

    "create and update an application with kafkaStreaming" in {
      val appRequest = CreateApplicationRequest(
        name = "kafka_app",
        namespace = None,
        executionGraph = ExecutionGraphRequest(
          stages = List(
            ExecutionStepRequest(
              modelVariants = List(
                ServiceCreationDescription(
                  modelVersionId = mv1.id,
                  weight = 100,
                  signatureName = "default"
                )
              )
            )
          )
        ),
        kafkaStreaming = Some(List(
          ApplicationKafkaStream(
            sourceTopic = "source",
            destinationTopic = "dest",
            consumerId = None,
            errorTopic = None
          )
        ))
      )
      eitherTAssert {
        for {
          app <- EitherT(managerServices.applicationManagementService.createApplication(
            appRequest.name,
            appRequest.namespace,
            appRequest.executionGraph,
            appRequest.kafkaStreaming.get
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

    "create and update an application contract" in {
      eitherTAssert {
        val appRequest = CreateApplicationRequest(
          name = "contract_app",
          namespace = None,
          executionGraph = ExecutionGraphRequest(
            stages = List(
              ExecutionStepRequest(
                modelVariants = List(
                  ServiceCreationDescription(
                    modelVersionId = mv1.id,
                    weight = 100,
                    signatureName = "default"
                  )
                )
              )
            )
          ),
          kafkaStreaming = None
        )
        for {
          app <- EitherT(managerServices.applicationManagementService.createApplication(
            appRequest.name,
            appRequest.namespace,
            appRequest.executionGraph,
            Seq.empty
          ))
          newGraph = ExecutionGraphRequest(
            stages = List(
              ExecutionStepRequest(
                modelVariants = List(
                  ServiceCreationDescription(
                    modelVersionId = mv2.id,
                    weight = 100,
                    signatureName = "not-default-spark"
                  )
                )
              )
            )
          )
          appNew <- EitherT(managerServices.applicationManagementService.updateApplication(
            app.id,
            app.name,
            app.namespace,
            newGraph,
            Seq.empty
          ))

          gotNewApp <- EitherT(managerServices.applicationManagementService.getApplication(appNew.id))
        } yield {
          assert(appNew === gotNewApp, gotNewApp)
          assert(appNew.contract.signatures.length === upload3.contract.get.signatures.length)
        }
      }
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()

    dockerClient.pull("hydrosphere/serving-runtime-dummy:latest")

    val f = for {
      d1 <- EitherT(managerServices.modelManagementService.uploadModel(uploadFile, upload1))
      completed1 <- EitherT.liftF[Future, HError, ModelVersion](d1.completedVersion)
      d2 <- EitherT(managerServices.modelManagementService.uploadModel(uploadFile, upload2))
      completed2 <- EitherT.liftF[Future, HError, ModelVersion](d2.completedVersion)
    } yield {
      println(s"UPLOADED: $completed1")
      println(s"UPLOADED: $completed2")
      mv1 = completed1
      mv2 = completed2
    }

    Await.result(f.value, 30 seconds)
  }
}