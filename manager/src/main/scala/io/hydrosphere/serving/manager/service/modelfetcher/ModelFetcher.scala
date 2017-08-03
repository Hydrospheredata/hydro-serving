package io.hydrosphere.serving.manager.service.modelfetcher

import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.Model
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging

/**
  *
  */
trait ModelFetcher {
  def fetch(source: ModelSource, directory: String): Option[Model]
}

object ModelFetcher extends Logging {
  private[this] val fetchers = List(
    SparkModelFetcher,
    TensorflowModelFetcher,
    ScikitModelFetcher
  )

  def getModel(source: ModelSource, folder: String) = {
    val res = fetchers
      .map(_.fetch(source, folder))

    val model = res
      .filter(_.isDefined)
      .map(_.get)
      .headOption
      .getOrElse {
        Model(-1, folder, "unknown", None, None, List.empty, List.empty, LocalDateTime.now(), LocalDateTime.now())
      }
    model
  }

  def getModels(source: ModelSource): Seq[Model] = {
    source.getSubDirs.map(getModel(source, _))
  }
}