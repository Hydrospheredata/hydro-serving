package io.hydrosphere.serving.manager.service.modelfetcher

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.api.ModelMetadata
import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelFetcher
import io.hydrosphere.serving.manager.model.api._
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging

/**
  *
  */
trait ModelFetcher {
  def fetch(source: ModelSource, directory: String): Option[ModelMetadata]
}

object ModelFetcher extends Logging {
  private[this] val fetchers = List(
    SparkModelFetcher,
    TensorflowModelFetcher,
    ScikitModelFetcher
  )

  def getModel(source: ModelSource, folder: String): ModelMetadata = {
    source.getAllFiles(folder)
    val res = fetchers
      .map(_.fetch(source, folder))

    val model = res.flatten
      .headOption
      .getOrElse {
        ModelMetadata(folder, ModelType.Unknown(), ModelContract())
      }
    model
  }

  def getModels(source: ModelSource): Seq[ModelMetadata] = {
    source.getSubDirs.map(getModel(source, _))
  }
}