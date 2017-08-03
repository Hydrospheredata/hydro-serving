package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.LocalModelSourceConfiguration
import io.hydrosphere.serving.manager.service.modelfetcher.{ScikitModelFetcher, SparkModelFetcher, TensorflowModelFetcher}
import io.hydrosphere.serving.manager.service.modelsource.LocalModelSource
import org.scalatest._

class FetcherTestSpecs extends FlatSpec with Matchers {
  val localSource = new LocalModelSource(LocalModelSourceConfiguration("test", "/Users/bulat/Documents/Dev/Provectus/hydro-serving/manager/src/test/resources/test_models"))

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

  }
}
