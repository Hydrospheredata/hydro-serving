package io.hydrosphere.serving.manager.it.service

import cats.data.EitherT
import cats.effect.IO
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.data_profile_types.DataProfileType
import io.hydrosphere.serving.manager.domain.DomainError
import io.hydrosphere.serving.manager.domain.application._
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.manager.it.FullIntegrationSpec
import io.hydrosphere.serving.tensorflow.types.DataType.DT_DOUBLE
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class ApplicationServiceITSpec extends FullIntegrationSpec with BeforeAndAfterAll {
  private val uploadFile = packModel("/models/dummy_model")
  private val signature = ModelSignature(
    signatureName = "not-default-spark",
    inputs = List(ModelField("test-input", None, DataProfileType.NONE, ModelField.TypeOrSubfields.Dtype(DT_DOUBLE))),
    outputs = List(ModelField("test-output", None, DataProfileType.NONE, ModelField.TypeOrSubfields.Dtype(DT_DOUBLE)))
  )
  private val upload1 = ModelUploadMetadata(
    name = "m1",
    runtime = dummyImage,
    contract = Some(ModelContract(
      predict = Some(signature)
    ))
  )
  private val upload2 = ModelUploadMetadata(
    name = "m2",
    runtime = dummyImage,
    contract = Some(ModelContract(
      predict = Some(signature)
    ))
  )
  private val upload3 = ModelUploadMetadata(
    name = "m3",
    runtime = dummyImage,
    contract = Some(ModelContract(
      predict = Some(signature)
    ))
  )

  var mv1: ModelVersion = _
  var mv2: ModelVersion = _
  var mv3: ModelVersion = _

  describe("Application service") {
    it("should create a simple application") {
      eitherTAssert {
        val create = CreateApplicationRequest(
          "simple-app",
          None,
          ExecutionGraphRequest(List(
            PipelineStageRequest(
              Seq(ModelVariantRequest(
                modelVersionId = mv1.id,
                weight = 100
              ))
            ))
          ),
          Option.empty
        )
        for {
          appResult <- EitherT(managerServices.appService.create(create))
        } yield {
          println(appResult)
          assert(appResult.started.name === "simple-app")
          assert(appResult.started.signature.inputs === mv1.modelContract.predict.get.inputs)
          assert(appResult.started.signature.outputs === mv1.modelContract.predict.get.outputs)
          val services = appResult.started.executionGraph.stages.flatMap(_.modelVariants)
          val service = services.head
          assert(service.weight === 100)
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
                    weight = 50
                  ),
                  ModelVariantRequest(
                    modelVersionId = mv1.id,
                    weight = 50
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
                  modelVersion = mv1,
                ),
                ModelVariant(
                  weight = 50,
                  modelVersion = mv1,
                )
              ),
              signature
            )
          )
        )
        for {
          app <- EitherT(managerServices.appService.create(appRequest))
        } yield {
          println(app)
          assert(app.started.name === appRequest.name)
          val services = app.started.executionGraph.stages.flatMap(_.modelVariants)
          val service1 = services.head
          val service2 = services.head
          assert(service1.weight === 50)
          assert(service1.modelVersion.id === mv1.id)
          assert(service2.weight === 50)
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
                  weight = 100
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
          app <- EitherT(managerServices.appService.create(appRequest))
          _ <- EitherT.liftF(app.completed.get)
          appNew <- EitherT(managerServices.appService.update(UpdateApplicationRequest(
            app.started.id,
            app.started.name,
            app.started.namespace,
            appRequest.executionGraph,
            Option.empty
          )))
          finishedNew <- EitherT.liftF(appNew.completed.get)
          gotNewApp <- EitherT.fromOptionF(managerRepositories.applicationRepository.get(appNew.started.id), DomainError.notFound(s"${appNew.started.id} app not found"))
        } yield {
          assert(finishedNew === gotNewApp)
          assert(appNew.started.kafkaStreaming.isEmpty, appNew)
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
                    weight = 100
                  )
                )
              )
            )
          ),
          kafkaStreaming = None
        )
        for {
          app <- EitherT(managerServices.appService.create(appRequest))
          newGraph = ExecutionGraphRequest(
            stages = List(
              PipelineStageRequest(
                modelVariants = List(
                  ModelVariantRequest(
                    modelVersionId = mv2.id,
                    weight = 100
                  )
                )
              )
            )
          )
          appNew <- EitherT(managerServices.appService.update(UpdateApplicationRequest(
            app.started.id,
            app.started.name,
            app.started.namespace,
            newGraph,
            Option.empty
          )))

          gotNewApp <- EitherT.fromOptionF(managerRepositories.applicationRepository.get(appNew.started.id), DomainError.notFound("app not found"))
        } yield {
          assert(appNew.started === gotNewApp, gotNewApp)
        }
      }
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()

    dockerClient.pull("hydrosphere/serving-runtime-dummy:latest")

    val f = for {
      d1 <- EitherT(managerServices.modelService.uploadModel(uploadFile, upload1))
      completed1 <- EitherT.liftF[IO, DomainError, ModelVersion](d1.completedVersion.get)
      d2 <- EitherT(managerServices.modelService.uploadModel(uploadFile, upload2))
      completed2 <- EitherT.liftF[IO, DomainError, ModelVersion](d2.completedVersion.get)
      d3 <- EitherT(managerServices.modelService.uploadModel(uploadFile, upload3))
      completed3 <- EitherT.liftF[IO, DomainError, ModelVersion](d3.completedVersion.get)
    } yield {
      println(s"UPLOADED: $completed1")
      println(s"UPLOADED: $completed2")
      println(s"UPLOADED: $completed3")
      mv1 = completed1
      mv2 = completed2
      mv3 = completed3
    }

    Await.result(f.value.unsafeToFuture(), 30 seconds)
  }
}