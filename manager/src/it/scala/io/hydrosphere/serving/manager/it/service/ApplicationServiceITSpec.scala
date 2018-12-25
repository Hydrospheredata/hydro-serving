package io.hydrosphere.serving.manager.it.service

import cats.data.EitherT
import cats.instances.all._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.api.http.controller.application._
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.domain.application.{ApplicationExecutionGraph, ApplicationKafkaStream, ApplicationStage, DetailedServiceDescription}
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.manager.it.FullIntegrationSpec
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
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
      signatures = Vector(
        ModelSignature(
          signatureName = "not-default-spark",
          inputs = Vector(ModelField("test-input")),
          outputs = Vector(ModelField("test-output"))
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
          val expectedGraph = ApplicationExecutionGraph(
            List(
              ApplicationStage(
                List(
                  DetailedServiceDescription(
                    weight = 100,
                    signature = None,
                    modelVersion = mv1,
                  )
                ),
                None
              )
            )
          )
          assert(appResult.name === "simple-app")
          assert(appResult.contract === mv1.modelContract)
          assert(appResult.executionGraph === expectedGraph)
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
                services = List(
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
            ApplicationStage(
              List(
                DetailedServiceDescription(
                  weight = 50,
                  signature = mv1.modelContract.signatures.find(_.signatureName == "default_spark"),
                  modelVersion = mv1,
                ),
                DetailedServiceDescription(
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
          assert(app.executionGraph === expectedGraph)
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
              services = List(
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
                services = List(
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
                services = List(
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
          assert(appNew.contract === upload3.contract.get)
        }
      }
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()

    dockerClient.pull("hydrosphere/serving-runtime-dummy:latest")

    val f = for {
      d1 <- EitherT(managerServices.modelManagementService.uploadModel(uploadFile, upload1))
      d2 <- EitherT(managerServices.modelManagementService.uploadModel(uploadFile, upload2))
    } yield {
      println(s"UPLOADED: $d1")
      println(s"UPLOADED: $d2")
      mv1 = d1
      mv2 = d2
      d2
    }

    Await.result(f.value, 30 seconds)
  }
}