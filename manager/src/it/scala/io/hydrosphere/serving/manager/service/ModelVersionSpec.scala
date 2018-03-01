package io.hydrosphere.serving.manager.service

import java.time.{Instant, LocalDateTime}

import akka.testkit.TestProbe
import io.hydrosphere.serving.manager.model.{LocalSourceParams, Model, ModelSourceConfig}
import io.hydrosphere.serving.manager.service.actors.RepositoryIndexActor
import io.hydrosphere.serving.manager.service.modelsource.events.FileCreated
import io.hydrosphere.serving.manager.service.modelsource.local.{LocalModelSource, LocalSourceDef}
import io.hydrosphere.serving.manager.test.FullIntegrationSpec
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class ModelVersionSpec extends FullIntegrationSpec with BeforeAndAfterAll {
  var dummy_1: Model = _
  var dummy_2: Model = _
  val source = ModelSourceConfig(1, "itsource", LocalSourceParams(getClass.getResource("/models").getPath))

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
        val f = new FileCreated(
          new LocalModelSource(LocalSourceDef.fromConfig(source)),
          "dummy_model/test",
          Instant.now(),
          "asdasd",
          LocalDateTime.now()
        )
        val indexProbe = TestProbe()
        system.eventStream.subscribe(indexProbe.ref, classOf[RepositoryIndexActor.IndexFinished])
        system.eventStream.publish(f)
        indexProbe.expectMsg(20.seconds, RepositoryIndexActor.IndexFinished("dummy_model", "itsource"))
        managerServices.modelManagementService.getModelAggregatedInfo(dummy_2.id).map{ maybeModel =>
          assert(maybeModel.isDefined)
          val model = maybeModel.get
          println(model)
          assert(model.nextVersion.isEmpty, model.nextVersion)
        }
        managerServices.modelManagementService.getModelAggregatedInfo(dummy_1.id).map{ maybeModel =>
          assert(maybeModel.isDefined)
          val model = maybeModel.get
          assert(model.nextVersion.isDefined)
          assert(model.nextVersion.get === 2)
          assert(model.lastModelVersion.get.modelVersion === 1)
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
    val indexProbe = TestProbe()
    system.eventStream.subscribe(indexProbe.ref, classOf[RepositoryIndexActor.IndexFinished])
    managerServices.sourceManagementService.addSource(source.toAux)
    indexProbe.expectMsgAllOf(
      20.seconds,
      RepositoryIndexActor.IndexFinished("dummy_model", "itsource"),
      RepositoryIndexActor.IndexFinished("dummy_model_2", "itsource")
    )
    val f1 = managerRepositories.modelRepository.get("dummy_model").map{ model =>
      dummy_1 = model.get
    }
    val f2 = managerRepositories.modelRepository.get("dummy_model_2").map{ model =>
      dummy_2 = model.get
    }
    Await.result(f1, 10.seconds)
    Await.result(f2, 10.seconds)
  }
}
