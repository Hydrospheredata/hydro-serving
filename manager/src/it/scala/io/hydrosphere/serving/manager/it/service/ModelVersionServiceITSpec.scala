package io.hydrosphere.serving.manager.it.service

import cats.data.EitherT
import cats.instances.all._
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.it.FullIntegrationSpec
import io.hydrosphere.serving.manager.model.db.Model
import io.hydrosphere.serving.manager.service.model_build.BuildModelRequest
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class ModelVersionServiceITSpec extends FullIntegrationSpec with BeforeAndAfterAll {
  implicit val awaitTimeout = 50.seconds

  val upload1 = ModelUpload(
    packModel("/models/dummy_model"),
    name = Some("m1")
  )
  val upload2 = ModelUpload(
    packModel("/models/dummy_model_2"),
    name = Some("m2")
  )

  var dummy_1: Model = _
  var dummy_2: Model = _

  "Model version" should {
    "calculate next model version" when {
      "model is fresh" in {
        managerServices.aggregatedInfoUtilityService.getModelAggregatedInfo(dummy_1.id).map{ maybeModel =>
          assert(maybeModel.isRight)
          val model = maybeModel.right.get
          assert(model.nextVersion.isDefined)
          assert(model.nextVersion.get === 1)
        }
      }

      "model was already build" in {
        for {
          r1 <- managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(dummy_1.id))
          cr1 = awaitVersion(r1.right.get.id)
          r2 <- managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(dummy_2.id))
          cr2 = awaitVersion(r2.right.get.id)
          modelInfo <- managerServices.aggregatedInfoUtilityService.getModelAggregatedInfo(dummy_1.id)
        } yield {
          assert(r1.isRight, r1)
          assert(r2.isRight, r2)
          assert(modelInfo.isRight, modelInfo)
          val model = modelInfo.right.get
          assert(model.nextVersion.isEmpty)
        }
      }

      "model changed after last version" in {
        managerServices.modelManagementService.uploadModel(upload1).flatMap { _ =>
          managerServices.aggregatedInfoUtilityService.getModelAggregatedInfo(dummy_2.id).map { maybeModel =>
            assert(maybeModel.isRight)
            val model = maybeModel.right.get
            println(model)
            assert(model.nextVersion.isEmpty, model.nextVersion)
          }
          managerServices.aggregatedInfoUtilityService.getModelAggregatedInfo(dummy_1.id).map { maybeModel =>
            assert(maybeModel.isRight)
            val model = maybeModel.right.get
            assert(model.nextVersion.isDefined)
            assert(model.nextVersion.get === 2)
            assert(model.lastModelVersion.get.modelVersion === 1)
          }
        }
      }

      "return info for all models" in {
        managerServices.aggregatedInfoUtilityService.allModelsAggregatedInfo().map{ info =>
          val d1Info = info.find(_.model.id == dummy_1.id).get
          val d2Info = info.find(_.model.id == dummy_2.id).get

          assert(d1Info.nextVersion.isDefined)
          assert(d1Info.nextVersion.get === 2)
          assert(d2Info.nextVersion.isEmpty, d2Info)
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
      println("UPLOADED:")
      println(d1)
      println(d2)
      dummy_1 = d1
      dummy_2 = d2
      d2
    }

    println(s"RESULT: ${Await.result(f.value, 1.minute)}")

    println(dummy_1)
    println(dummy_2)
  }
}
