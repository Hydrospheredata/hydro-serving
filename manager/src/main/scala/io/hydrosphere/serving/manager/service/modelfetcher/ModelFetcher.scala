package io.hydrosphere.serving.manager.service.modelfetcher

import io.hydrosphere.serving.manager.model.Model
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging

/**
  *
  */
trait ModelFetcher {

  def getModel(directory: String): Option[Model]

  def getModels: Seq[Model]
}

object ModelFetcher extends Logging {
  def getModels(source: ModelSource): Seq[Model] = source.getSubDirs.flatMap{ cat =>
    val fetcher = cat match {
      case "spark" => new SparkModelFetcher(source)
      case "scikit" => new ScikitModelFetcher(source)
      case r =>
        logger.error(s"Unknown runtime detected: $r")
        throw new IllegalArgumentException(s"Unknown runtime detected: $r")
    }
    fetcher.getModels
  }
}