package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.test.CommonIntegrationSpec
import io.hydrosphere.serving.manager.model.api.description._
import io.hydrosphere.serving.manager.model.api.ops.Implicits._
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest.BeforeAndAfterEach
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.api.{ContractBuilders, ModelType}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  *
  */
class ModelManagementServiceITSpec extends CommonIntegrationSpec with BeforeAndAfterEach {

  describe("Model") {
    it("should fetch all models") {
      val f = managerServices.modelManagementService.allModels().map(seq => {
        assert(seq.lengthCompare(1) === 0)
      })
      Await.result(f, 10.seconds)
    }

    it("should create model") {
      val request = CreateOrUpdateModelRequest(
        id = None,
        name = "Test222",
        source = "source",
        description = Some("SSS"),
        modelContract = ModelContract(),
        modelType = ModelType.Unknown()
      )
      val f = managerServices.modelManagementService.createModel(request).map{ model =>
        assert(model.name === request.name)
        assert(model.source === request.source)
        assert(model.description === request.description)
        assert(model.modelContract === request.modelContract)
        assert(model.updated !== null)
        assert(model.created !== null)
      }
      Await.result(f, 10.seconds)
    }

    it("should update a flat model contract") {
      val flatContract = ContractDescription(
        List(
          SignatureDescription(
            "sig1",
            inputs = List(
              FieldDescription("/in1", DataType.DT_STRING, None)
            ),
            outputs = List(
              FieldDescription("/out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          )
        )
      )

      val f = managerServices.modelManagementService.submitFlatContract(1000, flatContract)
      val maybeModel = Await.result(f, 10.seconds)
      assert(maybeModel.isDefined)

      val newF = managerRepositories.modelRepository.get(1000)
      val maybeNewModel = Await.result(newF, 10.seconds)
      assert(maybeNewModel.isDefined)
      val newModel = maybeNewModel.get
      assert(newModel.modelContract.flatten === flatContract)
    }

    it("should update a text model contract") {
      val contract = ModelContract(
        modelName = "",
        signatures = List(
          ModelSignature(
            "sig1",
            List(
              ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, None)
            ),
            List(
              ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          )
        )
      )

      val f = managerServices.modelManagementService.submitContract(1000, contract.toString())
      val maybeModel = Await.result(f, 10.seconds)
      assert(maybeModel.isDefined)

      val newF = managerRepositories.modelRepository.get(1000)
      val maybeNewModel = Await.result(newF, 10.seconds)
      assert(maybeNewModel.isDefined)
      val newModel = maybeNewModel.get
      assert(newModel.modelContract === contract)
    }

    it("should update a binary model contract") {
      val contract = ModelContract(
        modelName = "",
        signatures = List(
          ModelSignature(
            "sig1",
            List(
              ContractBuilders.simpleTensorModelField("in1", DataType.DT_STRING, None)
            ),
            List(
              ContractBuilders.simpleTensorModelField("out1", DataType.DT_DOUBLE, Some(List(-1)))
            )
          )
        )
      )

      val f = managerServices.modelManagementService.submitBinaryContract(1000, contract.toByteArray)
      val maybeModel = Await.result(f, 10.seconds)
      assert(maybeModel.isDefined)

      val newF = managerRepositories.modelRepository.get(1000)
      val maybeNewModel = Await.result(newF, 10.seconds)
      assert(maybeNewModel.isDefined)
      val newModel = maybeNewModel.get
      assert(newModel.modelContract === contract)
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
