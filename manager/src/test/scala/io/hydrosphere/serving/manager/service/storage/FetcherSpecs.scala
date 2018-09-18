package io.hydrosphere.serving.manager.service.storage

import java.nio.file.Paths

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.ContractBuilders
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.model.api.ModelType.ONNX
import io.hydrosphere.serving.manager.service.source.fetchers.spark.SparkModelFetcher
import io.hydrosphere.serving.manager.service.source.fetchers._
import io.hydrosphere.serving.manager.service.source.fetchers.keras.KerasFetcher
import io.hydrosphere.serving.manager.service.source.fetchers.tensorflow.TensorflowModelFetcher
import io.hydrosphere.serving.manager.service.source.storages.local.{LocalModelStorage, LocalModelStorageDefinition}
import io.hydrosphere.serving.manager.{GenericUnitTest, TestConstants}
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType

class FetcherSpecs extends GenericUnitTest {
  val localSource = new LocalModelStorage(LocalModelStorageDefinition("TEST", Paths.get(TestConstants.localModelsPath)))

  describe("Fallback") {
    it("should parse contract proto message") {
      val model = FallbackContractFetcher.fetch(localSource, "scikit_model")
      model shouldBe defined
      assert(model.get.modelType === ModelType.Unknown("unknown", "fallback"))
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

  describe("KerasFetcher") {
    it("should parse sequential model from .h5") {
      val expectedContract = ModelContract(
        "keras_fashion_mnist",
        Seq(ModelSignature(
          "infer",
          Seq(
            ContractBuilders.simpleTensorModelField("flatten_1", DataType.DT_FLOAT, TensorShape.mat(-1, 28, 28))
          ),
          Seq(
            ContractBuilders.simpleTensorModelField("dense_3", DataType.DT_FLOAT, TensorShape.mat(-1, 10))
          ))
        )
      )
      val fetchResult = KerasFetcher.fetch(localSource, "keras_model/sequential")
      assert(fetchResult.isDefined, fetchResult)
      val metadata = fetchResult.get
      println(metadata)
      assert(metadata.modelName === "keras_fashion_mnist")
      assert(metadata.modelType === ModelType.Unknown("keras", "2.1.6-tf"))
      assert(metadata.contract === expectedContract)
    }

    it("should parse functional model from .h5") {
      val expectedContract = ModelContract(
        "nonseq_model",
        Seq(ModelSignature(
          "infer",
          Seq(
            ContractBuilders.simpleTensorModelField("input_7", DataType.DT_FLOAT, TensorShape.mat(-1, 784))
          ),
          Seq(
            ContractBuilders.simpleTensorModelField("dense_20", DataType.DT_INVALID, TensorShape.mat(-1, 10)),
            ContractBuilders.simpleTensorModelField("dense_21", DataType.DT_INVALID, TensorShape.mat(-1, 10))
          ))
        )
      )
      val fetchResult = KerasFetcher.fetch(localSource, "keras_model/functional")
      assert(fetchResult.isDefined, fetchResult)
      val metadata = fetchResult.get
      println(metadata)
      assert(metadata.modelName === "nonseq_model")
      assert(metadata.modelType === ModelType.Unknown("keras", "2.2.2"))
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
