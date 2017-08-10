package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.test.CommonIntegrationSpec
import org.scalactic.source.Position
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  *
  */
class ModelManagementServiceSpec extends CommonIntegrationSpec with BeforeAndAfterEach {


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
        "source",
        runtimeTypeId = Some(1000),
        description = Some("SSS"),
        outputFields = Some(List("output")),
        inputFields = Some(List("input"))
      )
      val f = managerServices.modelManagementService.createModel(request).map(model => {
        assert(model.runtimeType.isDefined && model.runtimeType.get.id == request.runtimeTypeId.get)
        assert(model.name == request.name)
        assert(model.source == request.source)
        assert(model.description == request.description)
        assert(model.inputFields == request.inputFields.get)
        assert(model.outputFields == request.outputFields.get)
        assert(model.updated != null)
        assert(model.created != null)
      })
      Await.result(f, 10.seconds)
    }
  }

  /*
  id: Option[Long],
  name: String,
  source: String,
  runtimeTypeId: Option[Long],
  description: Option[String],
  outputFields: Option[List[String]],
  inputFields: Option[List[String]]
  */

  override protected def beforeEach(): Unit = {
    executeDBScript("/db/models.sql")
    super.beforeEach()
  }

  override protected def afterEach(): Unit = {
    executeDBScript("/db/clear.sql")
    super.afterEach()
  }
}
