package io.hydrosphere.serving.manager.service.source.fetchers

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.api.ModelMetadata
import io.hydrosphere.serving.manager.service.source.fetchers.spark.SparkModelFetcher
import io.hydrosphere.serving.manager.model.api._
import io.hydrosphere.serving.manager.service.source.storages.ModelStorage
import org.apache.logging.log4j.scala.Logging


trait ModelFetcher {
  def fetch(source: ModelStorage, directory: String): Option[ModelMetadata]
}

object ModelFetcher extends Logging {
  private[this] val fetchers = List(
    SparkModelFetcher,
    TensorflowModelFetcher,
    ScikitModelFetcher
  )

  def fetch(source: ModelStorage, folder: String): ModelMetadata = {
    source.getAllFiles(folder)
    val res = fetchers
      .map(_.fetch(source, folder))

    val model = res.flatten
      .headOption
      .getOrElse {
        ModelMetadata(folder, ModelType.Unknown("unknown"), ModelContract())
      }
    model
  }
}