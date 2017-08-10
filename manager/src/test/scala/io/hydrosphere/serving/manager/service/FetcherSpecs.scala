package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, S3ModelSourceConfiguration, TestConstants}
import io.hydrosphere.serving.manager.service.modelfetcher.{ModelFetcher, ScikitModelFetcher, SparkModelFetcher, TensorflowModelFetcher}
import io.hydrosphere.serving.manager.service.modelsource.{LocalModelSource, ModelSource, S3ModelSource}
import org.scalatest._

class FetcherSpecs extends FlatSpec with Matchers {
  val localSource = new LocalModelSource(LocalModelSourceConfiguration("test", TestConstants.localModelsPath))

  "Scikit model fetcher" should "parse correct scikit model" in {
    val model = ScikitModelFetcher.fetch(localSource, "scikit_model")
    model shouldBe defined
  }

  "Spark model fetcher" should "parse correct spark model" in {
    val model = SparkModelFetcher.fetch(localSource, "spark_model")
    model shouldBe defined
  }

  "Tensorflow model fetcher" should "parse correct tensorflow model" in {
    val model = TensorflowModelFetcher.fetch(localSource, "tensorflow_model")
    model shouldBe defined
  }

  "Fetcher chain" should "parse source" in {
    val models = ModelFetcher.getModels(localSource)
    models should not be empty
  }
}
