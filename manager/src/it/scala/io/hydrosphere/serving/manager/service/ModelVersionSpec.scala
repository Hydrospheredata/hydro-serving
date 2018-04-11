package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.model.db.{Model, ModelSourceConfig}
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.LocalSourceParams
import io.hydrosphere.serving.manager.test.FullIntegrationSpec
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class ModelVersionSpec extends FullIntegrationSpec with BeforeAndAfterAll {
  var dummy_1: Model = _
  var dummy_2: Model = _
  val source = ModelSourceConfig(1, "itsource", LocalSourceParams(None))

  "Model version" should {
    "calculate next model version" when {
      "model is fresh" in {
        managerServices.modelManagementService.getModelAggregatedInfo(dummy_1.id).map{ maybeModel =>
          assert(maybeModel.isDefined)
          val model = maybeModel.get
          assert(model.nextVersion.isDefined)
          assert(model.nextVersion.get === 1)
        }
      }

      "model was already build" in {
        for {
          _ <- managerServices.modelManagementService.buildModel(dummy_1.id)
          _ <- managerServices.modelManagementService.buildModel(dummy_2.id)
          modelInfo <- managerServices.modelManagementService.getModelAggregatedInfo(dummy_1.id)
        } yield {
          assert(modelInfo.isDefined)
          val model = modelInfo.get
          assert(model.nextVersion.isEmpty)
        }
      }

      "model changed after last version" in {
        managerServices.modelManagementService.indexModels(Set(dummy_1.id)).flatMap { _ =>
          managerServices.modelManagementService.getModelAggregatedInfo(dummy_2.id).map { maybeModel =>
            assert(maybeModel.isDefined)
            val model = maybeModel.get
            println(model)
            assert(model.nextVersion.isEmpty, model.nextVersion)
          }
          managerServices.modelManagementService.getModelAggregatedInfo(dummy_1.id).map { maybeModel =>
            assert(maybeModel.isDefined)
            val model = maybeModel.get
            assert(model.nextVersion.isDefined)
            assert(model.nextVersion.get === 2)
            assert(model.lastModelVersion.get.modelVersion === 1)
          }
        }
      }

      "return info for all models" in {
        managerServices.modelManagementService.allModelsAggregatedInfo().map{ info =>
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
    val sourceConf = ModelSourceConfig(1, "itsource", LocalSourceParams(Some(getClass.getResource("/models").getPath)))
    val f = managerServices.sourceManagementService.addSource(sourceConf).flatMap { _ =>
      managerServices.modelManagementService.addModel("itsource", "dummy_model").flatMap { d1 =>
        dummy_1 = d1.get
        managerServices.modelManagementService.addModel("itsource", "dummy_model_2").map { d2 =>
          dummy_2 = d2.get
          d2
        }
      }
    }

    Await.result(f, 30 seconds)
  }
}
