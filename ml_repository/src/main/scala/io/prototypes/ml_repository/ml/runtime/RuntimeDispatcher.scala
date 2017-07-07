package io.prototypes.ml_repository.ml.runtime

import java.io._

import io.prototypes.ml_repository.datasource.DataSource
import io.prototypes.ml_repository.ml.Model
import io.prototypes.ml_repository.ml.runtime.scikit.ScikitRuntime
import io.prototypes.ml_repository.ml.runtime.spark.SparkRuntime
import org.apache.logging.log4j.scala.Logging
/**
  * Created by Bulat on 01.06.2017.
  */
object RuntimeDispatcher extends Logging {
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
