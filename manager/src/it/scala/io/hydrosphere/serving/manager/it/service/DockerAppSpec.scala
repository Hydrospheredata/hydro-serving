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
import scala.collection.JavaConverters._
import scala.concurrent.duration._

class DockerAppSpec extends FullIntegrationSpec with BeforeAndAfterAll {
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

  describe("Application and Servable service") {
    it("should delete unused servables after deletion") {
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
          _ <- EitherT.liftF(appResult.completed.get)
          preCont <- EitherT.liftF(IO(dockerClient.listContainers()))
          _ <- EitherT.liftF(IO.pure(Thread.sleep(10000)))
          _ <- EitherT(managerServices.appService.delete(appResult.started.name))
          cont <- EitherT.liftF(IO(dockerClient.listContainers()))
        } yield {
          println("App containers:")
          preCont.forEach(println)
          println("---end of containers---")
          println("After containers:")
          cont.forEach(println)
          println("---end of containers---")
          assert(preCont.asScala !== cont.asScala)
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
    } yield {
      println(s"UPLOADED: $completed1")
      println(s"UPLOADED: $completed2")
      mv1 = completed1
      mv2 = completed2
    }

    Await.result(f.value.unsafeToFuture(), 30 seconds)
  }
}