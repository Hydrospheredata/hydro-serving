package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.test.CommonIntegrationSpec
import io.hydrosphere.serving.manager.model.api._
import org.scalactic.source.Position
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  *
  */
class ModelManagementServiceITSpec extends CommonIntegrationSpec with BeforeAndAfterEach {

  describe("Model") {
    it("Fetch all models") {
      val f = managerServices.modelManagementService.allModels().map(seq => {
        assert(seq.size == 1)
      })
      Await.result(f, 10.seconds)
    }

    it("Create model") {
      val request = CreateOrUpdateModelRequest(
        id = None,
        name = "Test222",
        source = "source",
        description = Some("SSS"),
        modelContract = ModelContract(),
        modelType = ModelType.Unknown()
      )
      val f = managerServices.modelManagementService.createModel(request).map{ model =>
        assert(model.name == request.name)
        assert(model.source == request.source)
        assert(model.description == request.description)
        assert(model.modelContract == request.modelContract)
        assert(model.updated != null)
        assert(model.created != null)
      }
      Await.result(f, 10.seconds)
    }
  }

  override protected def beforeEach(): Unit = {
    executeDBScript("/db/models.sql")
    super.beforeEach()
  }

  override protected def afterEach(): Unit = {
    executeDBScript("/db/clear.sql")
    super.afterEach()
  }
}
