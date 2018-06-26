package io.hydrosphere.serving.manager.service.storage

import java.nio.file.Paths

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.ContractBuilders
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.api.ModelType.ONNX
import io.hydrosphere.serving.manager.service.source.fetchers.spark.SparkModelFetcher
import io.hydrosphere.serving.manager.service.source.fetchers.{ModelFetcher, ONNXFetcher, ScikitModelFetcher, TensorflowModelFetcher}
import io.hydrosphere.serving.manager.service.source.storages.local.{LocalModelStorage, LocalModelStorageDefinition}
import io.hydrosphere.serving.manager.{GenericUnitTest, TestConstants}
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType

class FetcherSpecs extends GenericUnitTest {
  val localSource = new LocalModelStorage(LocalModelStorageDefinition("TEST", Paths.get(TestConstants.localModelsPath)))

  describe("Scikit model fetcher") {
    it("should parse correct scikit model") {
      val model = ScikitModelFetcher.fetch(localSource, "scikit_model")
      model shouldBe defined
      assert(model.get.modelType === ModelType.Scikit())
    }
  }

  describe("Spark model fetcher") {
    it("should parse correct spark model") {
      val model = SparkModelFetcher.fetch(localSource, "spark_model")
      model shouldBe defined
      assert(model.get.modelType === ModelType.Spark("2.1.1"))
    }
  }

  describe("Tensorflow model fetcher") {
    it("should parse correct tensorflow model") {
      val expectedSigs = Seq(
        ModelSignature(
          "serving_default",
          Seq(ModelField("images", TensorShape.mat(-1, 784).toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_FLOAT))),
          Seq(
            ModelField("labels", TensorShape.vector(-1).toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_INT64)),
            ModelField("labels2", TensorShape.vector(-1).toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_INT64)),
            ModelField("random", TensorShape.mat(2, 3).toProto, ModelField.TypeOrSubfields.Dtype(DataType.DT_FLOAT))
          )
        )
      )
      val modelResult = TensorflowModelFetcher.fetch(localSource, "tensorflow_model")
      modelResult shouldBe defined
      val model = modelResult.get
      println(model)
      assert(model.modelType === ModelType.Tensorflow("1.1.0"))
      assert(model.contract.signatures === expectedSigs)
    }
  }

  describe("ONNX fetcher") {
    it("should parse ONNX model") {
      val expectedContract = ModelContract(
        "mnist",
        Seq(ModelSignature(
          "infer",
          Seq(
            ContractBuilders.simpleTensorModelField("Input73", DataType.DT_FLOAT, TensorShape.mat(1, 1, 28, 28))
          ),
          Seq(
            ContractBuilders.simpleTensorModelField("Plus422_Output_0", DataType.DT_FLOAT, TensorShape.mat(1, 10))
          ))
        )
      )

      val fetchResult = ONNXFetcher.fetch(localSource, "onnx_mnist")
      assert(fetchResult.isDefined, fetchResult)
      val metadata = fetchResult.get
      println(metadata)
      assert(metadata.modelName === "mnist")
      assert(metadata.modelType === ONNX("CNTK", "2.4"))
      assert(metadata.contract === expectedContract)
    }
  }

  describe("Fetcher chain") {
    it("should parse tensorflow model") {
      val models = ModelFetcher.fetch(localSource, "tensorflow_model")
      assert(models.modelType === ModelType.Tensorflow("1.1.0"))
    }
  }
}
