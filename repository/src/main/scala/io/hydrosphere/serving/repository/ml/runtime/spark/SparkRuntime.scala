package io.prototypes.ml_repository.ml.runtime.spark

import java.io.FileNotFoundException
import java.nio.file.Files

import io.hydrosphere.serving.repository.datasource.DataSource
import io.hydrosphere.serving.repository.ml.{Model, Runtime}
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._
/**
  * Created by Bulat on 31.05.2017.
  */
class SparkRuntime(val source: DataSource) extends Runtime with Logging {
  private[this] val inputCols = Array("inputCol", "featuresCol")
  private[this] val outputCols = Array("outputCol", "predictionCol", "probabilityCol", "rawPredictionCol")
  private[this] val labelCols = Array("labelCol")

  private def getRuntimeName(sparkMetadata: SparkMetadata): String = {
    "spark"
  }

  private def getStageMetadata(model: String, stage: String): SparkMetadata = {
    val metaFile = source.getReadableFile("spark", model, s"stages/$stage/metadata/part-00000")
    val metaStr = Files.readAllLines(metaFile.toPath).mkString
    SparkMetadata.fromJson(metaStr)
  }

  private def getMetadata(model: String): SparkMetadata = {
    val metaFile = source.getReadableFile("spark", model, "metadata/part-00000")
    val metaStr = Files.readAllLines(metaFile.toPath).mkString
    SparkMetadata.fromJson(metaStr)
  }

  private def getInputCols(stagesMetadata: Seq[SparkMetadata]): List[String] = {
    val inputs = stagesMetadata.flatMap(s => SparkMetadata.extractParams(s, inputCols))
    val outputs = stagesMetadata.flatMap(s => SparkMetadata.extractParams(s, outputCols))
    val labels = stagesMetadata.flatMap(s => SparkMetadata.extractParams(s, labelCols))

    if(labels.isEmpty) {
      inputs.diff(outputs).toList
    } else {
      val trains = stagesMetadata.filter { stage =>
        val outs = SparkMetadata.extractParams(stage, outputCols)
        val flag = outs.containsSlice(labels)
        flag
      }.flatMap(s => SparkMetadata.extractParams(s, inputCols))

      inputs.diff(outputs).diff(trains).toList
    }
  }

  private def getOutputCols(stagesMetadata: Seq[SparkMetadata]): List[String] = {
    val inputs = stagesMetadata.flatMap(s => SparkMetadata.extractParams(s, inputCols))
    val outputs = stagesMetadata.flatMap(s => SparkMetadata.extractParams(s, outputCols))
    outputs.diff(inputs).toList
  }

  override def getModel(directory: String): Option[Model] = {
    try {
      val fullPath = s"spark/$directory"
      val stagesDir = s"$fullPath/stages"

      val pipelineMetadata = getMetadata(directory)
      logger.debug(s"Pipeline: $pipelineMetadata")
      val stagesMetadata = source.getSubDirs(stagesDir).map { stage =>
        getStageMetadata(directory, stage)
      }
      logger.debug(s"Stages: $stagesMetadata")

      val model = Model(
        directory,
        getRuntimeName(pipelineMetadata),
        getInputCols(stagesMetadata),
        getOutputCols(stagesMetadata)
      )
      Some(model)
    } catch {
      case e: FileNotFoundException =>
        logger.warn(s"$source $directory in not a valid SparkML model")
        None
    }
  }

  def getModels: Seq[Model] = {
    val models = source.getSubDirs("spark")
    models.map(getModel).filter(_.isDefined).map(_.get)
  }
}




