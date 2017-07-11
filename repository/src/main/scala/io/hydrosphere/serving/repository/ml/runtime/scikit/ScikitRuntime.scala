package io.hydrosphere.serving.repository.ml.runtime.scikit

import java.io._
import java.nio.file.Files

import io.hydrosphere.serving.repository.datasource.DataSource
import io.hydrosphere.serving.repository.ml.{Model, Runtime}
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._

/**
  * Created by Bulat on 31.05.2017.
  */
class ScikitRuntime(val source: DataSource) extends Runtime with Logging {
  private def getMetadata(modelName: String): ScikitMetadata = {
    val metaFile = source.getReadableFile("scikit", modelName, "metadata.json")
    val metaStr = Files.readAllLines(metaFile.toPath).mkString
    ScikitMetadata.fromJson(metaStr)
  }

  override def getModel(directory: String): Option[Model] = {
    try {
      val metadata = getMetadata(directory)
      val model = Model(directory, "scikit", metadata.inputs, metadata.outputs)
      Some(model)
    } catch {
      case e: FileNotFoundException =>
        logger.warn(s"$directory in not a valid SKLearn model")
        None
    }
  }

  override def getModels: Seq[Model] = {
    val models = source.getSubDirs("scikit")
    models.map(getModel).filter(_.isDefined).map(_.get)
  }
}
