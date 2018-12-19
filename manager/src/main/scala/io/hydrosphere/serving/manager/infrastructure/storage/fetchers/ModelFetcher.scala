package io.hydrosphere.serving.manager.infrastructure.storage.fetchers

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.infrastructure.storage.ModelStorage
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.keras.KerasFetcher
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark.SparkModelFetcher
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.tensorflow.TensorflowModelFetcher
import io.hydrosphere.serving.model.api.{ModelMetadata, ModelType}
import org.apache.logging.log4j.scala.Logging


trait ModelFetcher {
  def fetch(source: ModelStorage, directory: String): Option[ModelMetadata]
}

object ModelFetcher extends Logging {
  private[this] val fetchers = List(
    SparkModelFetcher,
    TensorflowModelFetcher,
    KerasFetcher,
    ONNXFetcher,
    FallbackContractFetcher
  )

  def fetch(source: ModelStorage, folder: String): ModelMetadata = {
    source.getAllFiles(folder)
    val res = fetchers
      .map(_.fetch(source, folder))

    val model = res.flatten
      .headOption
      .getOrElse {
        ModelMetadata(folder, ModelType.Unknown("unknown", "unknown"), ModelContract())
      }
    model
  }
}