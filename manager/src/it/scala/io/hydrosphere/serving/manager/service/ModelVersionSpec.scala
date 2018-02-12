package io.hydrosphere.serving.manager.service

import java.time.Instant

import akka.testkit.TestProbe
import io.hydrosphere.serving.manager.model.{LocalSourceParams, ModelSourceConfig}
import io.hydrosphere.serving.manager.service.actors.RepositoryIndexActor
import io.hydrosphere.serving.manager.service.modelsource.FileDetected
import io.hydrosphere.serving.manager.service.modelsource.local.{LocalModelSource, LocalSourceDef}
import io.hydrosphere.serving.manager.test.FullIntegrationSpec
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._

class ModelVersionSpec extends FullIntegrationSpec with BeforeAndAfterAll {
  "Model version" should {
    "calculate next model version" when {
      "model is fresh" in {
        managerServices.modelManagementService.getModelAggregatedInfo(1).map{ maybeModel =>
          assert(maybeModel.isDefined)
          val model = maybeModel.get
          assert(model.nextVersion.isDefined)
          assert(model.nextVersion.get === 1)
        }
      }
      "model was already build" in {
        for {
          _ <- managerServices.modelManagementService.buildModel(1)
          modelInfo <- managerServices.modelManagementService.getModelAggregatedInfo(1)
        } yield {
          assert(modelInfo.isDefined)
          val model = modelInfo.get
          assert(model.nextVersion.isEmpty)
        }
      }
      "model changed after last version" in {
        val f = new FileDetected(
          new LocalModelSource(LocalSourceDef.fromConfig(source)),
          "dummy_model/test",
          Instant.now(),
          "asdasd"
        )
        val indexProbe = TestProbe()
        system.eventStream.subscribe(indexProbe.ref, classOf[RepositoryIndexActor.IndexFinished])
        system.eventStream.publish(f)
        indexProbe.expectMsg(20.seconds, RepositoryIndexActor.IndexFinished("dummy_model", "itsource"))
        managerServices.modelManagementService.getModelAggregatedInfo(1).map{ maybeModel =>
          assert(maybeModel.isDefined)
          val model = maybeModel.get
          assert(model.nextVersion.isDefined)
          assert(model.nextVersion.get === 2)
          assert(model.lastModelVersion.get.modelVersion === 1)
        }
      }
    }
  }


  val source = ModelSourceConfig(1, "itsource", LocalSourceParams(getClass.getResource("/models").getPath))

  override def beforeAll(): Unit = {
    super.beforeAll()
    val indexProbe = TestProbe()
    system.eventStream.subscribe(indexProbe.ref, classOf[RepositoryIndexActor.IndexFinished])
    managerServices.sourceManagementService.addSource(source.toAux)
    indexProbe.expectMsg(15.seconds, RepositoryIndexActor.IndexFinished("dummy_model", "itsource"))
  }
}
