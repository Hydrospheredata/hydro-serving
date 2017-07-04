package io.hydrosphere.serving.repository.runtime

import java.io._

import io.hydrosphere.serving.repository.Model
import io.hydrosphere.serving.repository.runtime.scikit.ScikitRuntime
import io.hydrosphere.serving.repository.runtime.spark.SparkRuntime
import org.apache.logging.log4j.scala.Logging
/**
  * Created by Bulat on 01.06.2017.
  */
object RuntimeDispatcher extends Logging {
  def getModels(directories: Seq[File]): Seq[Model] = directories.flatMap{ dir =>
    val runtime = dir.getName match {
      case "spark" => new SparkRuntime(dir)
      case "scikit" => new ScikitRuntime(dir)
      case r =>
        logger.error(s"Unknown runtime detected: $r")
        throw new IllegalArgumentException(s"Unknown runtime detected: $r")
    }
    runtime.getModels
  }
}
