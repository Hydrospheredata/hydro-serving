package io.hydrosphere.serving.manager.storage

import java.nio.file.Paths

import cats.Id
import cats.effect.IO
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.contract.utils.ContractBuilders
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.infrastructure.storage.StorageOps
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers._
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.keras.KerasFetcher
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark.SparkModelFetcher
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.tensorflow.TensorflowModelFetcher
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.model.api.ModelType.ONNX
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType

class FetcherSpecs extends GenericUnitTest {

  describe("Fallback") {
    it("should parse contract proto message") {
      val ops = mock[StorageOps[Id]]
      val fetcher = new FallbackContractFetcher[Id](ops)
      val model = fetcher.fetch(Paths.get("fallback"))
      model shouldBe defined
      assert(model.get.modelName === "fallback")
    }
  }

  describe("Spark model fetcher") {
    it("should parse correct spark model") {
      val ops = mock[StorageOps[Id]]
      val fetcher = new SparkModelFetcher[Id](ops)
      val model = fetcher.fetch(Paths.get("spark-model"))
      model shouldBe defined
      assert(model.get.modelName === "spark-model")
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
      val ops = mock[StorageOps[Id]]
      val fetcher = new TensorflowModelFetcher[Id](ops)
      val modelResult = fetcher.fetch(Paths.get("tensorflow_model"))
      modelResult shouldBe defined
      val model = modelResult.get
      println(model)
      assert(model.modelContract.signatures === expectedSigs)
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

      val ops = mock[StorageOps[Id]]
      val fetcher = new ONNXFetcher[Id](ops)
      val fetchResult = fetcher.fetch(Paths.get("onnx_mnist"))
      assert(fetchResult.isDefined, fetchResult)
      val metadata = fetchResult.get
      println(metadata)
      assert(metadata.modelName === "mnist")
      assert(metadata.modelContract === expectedContract)
    }
  }

  describe("KerasFetcher") {
    it("should parse sequential model from .h5") {
      ioAssert {
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
        val ops = mock[StorageOps[IO]]
        val fetcher = new KerasFetcher[IO](ops)
        val fres = fetcher.fetch(Paths.get("keras_model/sequential"))
        fres.map { fetchResult =>
          assert(fetchResult.isDefined, fetchResult)
          val metadata = fetchResult.get
          println(metadata)
          assert(metadata.modelName === "keras_fashion_mnist")
          assert(metadata.modelContract === expectedContract)
        }
      }
    }

    it("should parse functional model from .h5") {
      ioAssert {
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
        val ops = mock[StorageOps[IO]]
        val fetcher = new KerasFetcher[IO](ops)
        val fres = fetcher.fetch(Paths.get("keras_model/functional"))
        fres.map { fetchResult =>
          assert(fetchResult.isDefined, fetchResult)
          val metadata = fetchResult.get
          println(metadata)
          assert(metadata.modelName === "nonseq_model")
          assert(metadata.modelContract === expectedContract)
        }
      }
    }
  }

  describe("Default fetcher") {
    it("should parse tensorflow model") {
      ioAssert {
        val ops = mock[StorageOps[IO]]
        val defaultFetcher = ModelFetcher.default[IO](ops)
        defaultFetcher.fetch(Paths.get("tensorflow_model")).map { model =>
          assert(model.isDefined, model)
        }
      }
    }
  }
}
