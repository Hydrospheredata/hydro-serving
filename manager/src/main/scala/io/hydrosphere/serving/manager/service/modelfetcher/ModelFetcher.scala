package io.hydrosphere.serving.manager.service.modelfetcher

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.Model
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
        ModelMetadata(folder, None, List.empty, List.empty)
      }
    model
  }

  def getModels(source: ModelSource): Seq[ModelMetadata] = {
    source.getSubDirs.map(getModel(source, _))
  }
}