package io.hydrosphere.serving.manager.storage


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
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType

class FetcherSpecs extends GenericUnitTest {
  val ops = StorageOps.default[IO]

  def getModel(modelName: String) = {
    getTestResourcePath("test_models").resolve(modelName)
  }

  describe("Fallback") {
    it("should parse contract proto message") {
      ioAssert {
        val fetcher = new FallbackContractFetcher(ops)
        fetcher.fetch(getModel("scikit_model")).map { model =>
          model shouldBe defined
          assert(model.get.modelName === "scikit_model")
        }
      }
    }
  }

  describe("Spark model fetcher") {
    it("should parse correct spark model") {
      ioAssert {
        val fetcher = new SparkModelFetcher(ops)
        fetcher.fetch(getModel("spark_model")).map { model =>
          model shouldBe defined
          assert(model.get.modelName === "spark_model")
          assert(model.get.metadata === Map(
            "class" -> "org.apache.spark.ml.PipelineModel",
            "timestamp" -> "1497440372794",
            "sparkVersion" -> "2.1.1",
            "uid" -> "PipelineModel_4ccbbca3d107857d3ed8"
          ))
        }
      }
    }
  }

  describe("Tensorflow model fetcher") {
    it("should parse correct tensorflow model") {
      ioAssert {
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
        val fetcher = new TensorflowModelFetcher(ops)
        fetcher.fetch(getModel("tensorflow_model")).map { modelResult =>
          modelResult shouldBe defined
          val model = modelResult.get
          assert(model.modelContract.signatures === expectedSigs)
          assert(model.metadata === Map(
            "0/tagsCount" -> "1",
            "0/tensorflowGitVersion" -> "b'unknown'",
            "0/strippedDefaultAttrs" -> "false",
            "0/serializedSize" -> "55589",
            "0/assetFilesCount" -> "0",
            "0/signatureCount" -> "1",
            "0/tensorflowVersion" -> "1.1.0",
            "metaGraphsCount" -> "1",
            "0/collectionsCount" -> "4"
          ))
        }
      }
    }
  }

  describe("ONNX fetcher") {
    it("should parse ONNX model") {
      ioAssert {
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

        val fetcher = new ONNXFetcher(ops)
        fetcher.fetch(getModel("onnx_mnist")).map { fetchResult =>
          assert(fetchResult.isDefined, fetchResult)
          val metadata = fetchResult.get
          println(metadata)
          assert(metadata.modelName === "mnist")
          assert(metadata.modelContract === expectedContract)
          assert(metadata.metadata === Map(
            "producerVersion" -> "2.4",
            "producerName" -> "CNTK",
            "modelVersion" -> "1",
            "irVersion" -> "3"
          ))
        }
      }
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
        val fetcher = new KerasFetcher[IO](ops)
        val fres = fetcher.fetch(getModel("keras_model/sequential"))
        fres.map { fetchResult =>
          assert(fetchResult.isDefined, fetchResult)
          val metadata = fetchResult.get
          println(metadata)
          assert(metadata.modelName === "keras_fashion_mnist")
          assert(metadata.modelContract === expectedContract)
          assert(metadata.metadata === Map())
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
        val fetcher = new KerasFetcher[IO](ops)
        val fres = fetcher.fetch(getModel("keras_model/functional"))
        fres.map { fetchResult =>
          assert(fetchResult.isDefined, fetchResult)
          val metadata = fetchResult.get
          println(metadata)
          assert(metadata.modelName === "nonseq_model")
          assert(metadata.modelContract === expectedContract)
          assert(metadata.metadata === Map())
        }
      }
    }
  }

  describe("Default fetcher") {
    it("should parse tensorflow model") {
      ioAssert {
        val defaultFetcher = ModelFetcher.default[IO](ops)
        defaultFetcher.fetch(getModel("tensorflow_model")).map { model =>
          assert(model.isDefined, model)
        }
      }
    }
  }
}