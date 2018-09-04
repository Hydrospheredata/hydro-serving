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
  val upload3 = ModelUpload(
    packModel("/models/dummy_model_2"),
    name = Some("m3")
  )

  var dummy1: Model = _
  var dummy2: Model = _
  var dummy3: Model = _

  "Model version" should {
    "calculate next model version" when {
      "model is fresh" in {
        managerServices.aggregatedInfoUtilityService.getModelAggregatedInfo(dummy1.id).map{ maybeModel =>
          assert(maybeModel.isRight)
          val model = maybeModel.right.get
          assert(model.nextVersion.isDefined)
          assert(model.nextVersion.get === 1)
        }
      }

      "model was already build" in {
        eitherTAssert {
          for {
            r1 <- EitherT(managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(dummy1.id)))
            _ <- EitherT.liftF(r1.future)
            r2 <- EitherT(managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(dummy2.id)))
            _ <- EitherT.liftF(r2.future)
            modelInfo <- EitherT(managerServices.aggregatedInfoUtilityService.getModelAggregatedInfo(dummy1.id))
          } yield {
            assert(modelInfo.nextVersion.isEmpty)
          }
        }
      }

      "model has several versions" in {
        eitherTAssert {
          for {
            r1 <- EitherT(managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(dummy3.id)))
            f1 <- EitherT.liftF(r1.future)

            r2 <- EitherT(managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(dummy3.id)))
            f2 <- EitherT.liftF(r2.future)

            r3 <- EitherT(managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(dummy3.id)))
            f3 <- EitherT.liftF(r3.future)

            r4 <- EitherT(managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(dummy3.id)))
            f4 <- EitherT.liftF(r4.future)
          } yield {
            assert(f1.modelVersion === 1)
            assert(f2.modelVersion === 2)
            assert(f3.modelVersion === 3)
            assert(f4.modelVersion === 4)
          }
        }
      }

      "model changed after last version" in {
        managerServices.modelManagementService.uploadModel(upload1).flatMap { _ =>
          managerServices.aggregatedInfoUtilityService.getModelAggregatedInfo(dummy2.id).map { maybeModel =>
            assert(maybeModel.isRight)
            val model = maybeModel.right.get
            println(model)
            assert(model.nextVersion.isEmpty, model.nextVersion)
          }
          managerServices.aggregatedInfoUtilityService.getModelAggregatedInfo(dummy1.id).map { maybeModel =>
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
          val d1Info = info.find(_.model.id == dummy1.id).get
          val d2Info = info.find(_.model.id == dummy2.id).get

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
      d3 <- EitherT(managerServices.modelManagementService.uploadModel(upload3))
    } yield {
      println("UPLOADED:")
      println(d1)
      println(d2)
      println(d3)
      dummy1 = d1
      dummy2 = d2
      dummy3 = d3
      d3
    }

    println(s"RESULT: ${Await.result(f.value, 1.minute)}")

    println(dummy1)
    println(dummy2)
    println(dummy3)
  }
}
