package io.hydrosphere.serving.manager.service.modelfetcher

import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelFetcher
import io.hydrosphere.serving.manager.service.modelsource.local.{LocalModelSource, LocalSourceDef}
import io.hydrosphere.serving.manager.TestConstants
import io.hydrosphere.serving.manager.model.api.ModelType
import org.scalatest._

class FetcherSpecs extends FlatSpec with Matchers {
  val localSource = new LocalModelSource(LocalSourceDef("TEST", TestConstants.localModelsPath))

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
    val model = TensorflowModelFetcher.fetch(localSource, "tensorflow_model")
    model shouldBe defined
    assert(model.get.modelType === ModelType.Tensorflow())
  }

  "Fetcher chain" should "parse source" in {
    val models = ModelFetcher.getModels(localSource)
    models should not be empty
  }
}
