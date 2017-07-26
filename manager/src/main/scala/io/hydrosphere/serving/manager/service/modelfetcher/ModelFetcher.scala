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

  def getModels(source: ModelSource): Seq[Model] = {
    source.getSubDirs.map { folder =>
      val fetch_results = fetchers.map(_.fetch(source, folder))
      val model = fetch_results.filter(_.isDefined).head
      model.getOrElse {
        Model(-1, folder, "unknown", None, None, List.empty, List.empty, LocalDateTime.now(), LocalDateTime.now())
      }
    }
  }
}