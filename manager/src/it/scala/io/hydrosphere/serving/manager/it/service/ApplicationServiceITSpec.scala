package io.hydrosphere.serving.manager.it.service

import cats.data.EitherT
import cats.instances.all._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.domain.application._
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.manager.it.FullIntegrationSpec
import io.hydrosphere.serving.model.api.Result.HError
import io.hydrosphere.serving.tensorflow.types.DataType.DT_DOUBLE
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class ApplicationServiceITSpec extends FullIntegrationSpec with BeforeAndAfterAll {
  private val uploadFile = packModel("/models/dummy_model")
  private val signature = ModelSignature(
    signatureName = "not-default-spark",
    inputs = List(ModelField("test-input", None, ModelField.TypeOrSubfields.Dtype(DT_DOUBLE))),
    outputs = List(ModelField("test-output", None, ModelField.TypeOrSubfields.Dtype(DT_DOUBLE)))
  )
  private val upload1 = ModelUploadMetadata(
    name = Some("m1"),
    runtime = dummyImage,
    contract = Some(ModelContract(
      modelName = "m1",
      signatures = List(signature)
    ))
  )
  private val upload2 = ModelUploadMetadata(
    name = Some("m2"),
    runtime = dummyImage,
    contract = Some(ModelContract(
      modelName = "m2",
      signatures = List(signature)
    ))
  )
  private val upload3 = ModelUploadMetadata(
    name = Some("m3"),
    runtime = dummyImage,
    contract = Some(ModelContract(
      modelName = "m3",
      signatures = List(signature)
    ))
  )

  var mv1: ModelVersion = _
  var mv2: ModelVersion = _

  describe("Application service") {
    it("should create a simple application") {
      eitherTAssert {
        for {
          appResult <- EitherT(managerServices.applicationManagementService.createApplication(
            "simple-app",
            None,
            ExecutionGraphRequest(Seq(
              PipelineStageRequest(
                Seq(ModelVariantRequest(
                  modelVersionId = mv1.id,
                  weight = 100,
                  signatureName = "not-default-spark"
                ))
              ))
            ),
            Seq.empty
          ))
        } yield {
          println(appResult)
          assert(appResult.name === "simple-app")
          assert(appResult.signature.inputs === mv1.modelContract.signatures.head.inputs)
          assert(appResult.signature.outputs === mv1.modelContract.signatures.head.outputs)
          val services = appResult.executionGraph.stages.flatMap(_.modelVariants)
          val service = services.head
          assert(service.weight === 100)
          assert(service.signature === mv1.modelContract.signatures.head)
          assert(service.modelVersion.id === mv1.id)
        }
      }
    }

    it("should create a multi-service stage") {
      eitherTAssert {
        val appRequest = CreateApplicationRequest(
          name = "MultiServiceStage",
          namespace = None,
          executionGraph = ExecutionGraphRequest(
            stages = List(
              PipelineStageRequest(
                modelVariants = List(
                  ModelVariantRequest(
                    modelVersionId = mv1.id,
                    weight = 50,
                    signatureName = "not-default-spark"
                  ),
                  ModelVariantRequest(
                    modelVersionId = mv1.id,
                    weight = 50,
                    signatureName = "not-default-spark"
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
                ModelVariant(
                  weight = 50,
                  signature = signature,
                  modelVersion = mv1,
                ),
                ModelVariant(
                  weight = 50,
                  signature = signature,
                  modelVersion = mv1,
                )
              ),
              signature
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
          val services = app.executionGraph.stages.flatMap(_.modelVariants)
          val service1 = services.head
          val service2 = services.head
          assert(service1.weight === 50)
          assert(service1.signature.signatureName === "not-default-spark")
          assert(service1.modelVersion.id === mv1.id)
          assert(service2.weight === 50)
          assert(service2.signature.signatureName === "not-default-spark")
          assert(service2.modelVersion.id === mv1.id)
        }
      }
    }

    it("should create and update an application with kafkaStreaming") {
      val appRequest = CreateApplicationRequest(
        name = "kafka_app",
        namespace = None,
        executionGraph = ExecutionGraphRequest(
          stages = List(
            PipelineStageRequest(
              modelVariants = List(
                ModelVariantRequest(
                  modelVersionId = mv1.id,
                  weight = 100,
                  signatureName = "not-default-spark"
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

    it("should create and update an application contract") {
      eitherTAssert {
        val appRequest = CreateApplicationRequest(
          name = "contract_app",
          namespace = None,
          executionGraph = ExecutionGraphRequest(
            stages = List(
              PipelineStageRequest(
                modelVariants = List(
                  ModelVariantRequest(
                    modelVersionId = mv1.id,
                    weight = 100,
                    signatureName = "not-default-spark"
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
              PipelineStageRequest(
                modelVariants = List(
                  ModelVariantRequest(
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