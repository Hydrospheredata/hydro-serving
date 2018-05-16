package io.hydrosphere.serving.manager.service.storage.fetchers

import java.nio.file.Paths

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.TestConstants
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.service.source.fetchers.{ModelFetcher, ScikitModelFetcher, TensorflowModelFetcher}
import io.hydrosphere.serving.manager.service.source.fetchers.spark.SparkModelFetcher
import io.hydrosphere.serving.manager.service.source.storages.local.{LocalModelStorage, LocalModelStorageDefinition}
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.types.DataType
import org.scalatest._

class FetcherSpecs extends FlatSpec with Matchers {
  val localSource = new LocalModelStorage(LocalModelStorageDefinition("TEST", Paths.get(TestConstants.localModelsPath)))

  "Scikit model fetcher" should "parse correct scikit model" in {
    val model = ScikitModelFetcher.fetch(localSource, "scikit_model")
    model shouldBe defined
    assert(model.get.modelType === ModelType.Scikit())
  }

  "Spark model fetcher" should "parse correct spark model" in {
    val model = SparkModelFetcher.fetch(localSource, "spark_model")
    model shouldBe defined
    assert(model.get.modelType === ModelType.Spark("2.1.1"))
  }

  "Tensorflow model fetcher" should "parse correct tensorflow model" in {
    val expectedSigs = Seq(
      ModelSignature(
        "tensorflow/serving/predict",
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

  "Fetcher chain" should "parse tensorflow model" in {
    val models = ModelFetcher.fetch(localSource, "tensorflow_model")
    assert(models.modelType === ModelType.Tensorflow("1.1.0"))
  }
}
