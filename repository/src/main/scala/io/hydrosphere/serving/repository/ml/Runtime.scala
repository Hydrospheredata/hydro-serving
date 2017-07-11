package io.hydrosphere.serving.repository.ml

import io.hydrosphere.serving.repository.datasource.DataSource
import io.hydrosphere.serving.repository.ml.runtime.scikit.ScikitRuntime
import io.prototypes.ml_repository.ml.runtime.spark.SparkRuntime
import org.apache.logging.log4j.scala.Logging

/**
  * Created by Bulat on 26.05.2017.
  */
trait Runtime {
  def getModel(directory: String): Option[Model]
  def getModels: Seq[Model]
}

object Runtime extends Logging {
  def getModels(source: DataSource): Seq[Model] = source.getSubDirs.flatMap{ cat =>
    val runtime = cat match {
      case "spark" => new SparkRuntime(source)
      case "scikit" => new ScikitRuntime(source)
      case r =>
        logger.error(s"Unknown runtime detected: $r")
        throw new IllegalArgumentException(s"Unknown runtime detected: $r")
    }
    runtime.getModels
  }
}