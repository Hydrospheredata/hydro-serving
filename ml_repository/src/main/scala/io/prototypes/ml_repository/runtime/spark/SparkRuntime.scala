package io.prototypes.ml_repository.runtime.spark

import java.io.{File, FileNotFoundException}
import java.nio.file.Paths

import io.prototypes.ml_repository.Model
import io.prototypes.ml_repository.runtime.MLRuntime
import io.prototypes.ml_repository.utils.FileUtils._
import org.apache.logging.log4j.scala.Logging

import scala.io.Source

/**
  * Created by Bulat on 31.05.2017.
  */
class SparkRuntime(val sparkDir: File) extends MLRuntime with Logging {
  private def getRuntimeName(sparkMetadata: SparkMetadata): String = {
    //s"spark-${sparkMetadata.sparkVersion}"
    "spark"
  }

  private def getMetadata(directory: File): SparkMetadata = {
    val metadataPath = Paths.get(directory.getAbsolutePath, "metadata", "part-00000")
    val metadataSrc = Source.fromFile(metadataPath.toString)
    val metadataStr = try metadataSrc.getLines mkString "/n" finally metadataSrc.close()
    SparkMetadata.fromJson(metadataStr)
  }

  private[this] val inputCols = Array("inputCol", "featuresCol")
  private[this] val outputCols = Array("outputCol", "predictionCol", "probabilityCol", "rawPredictionCol")
  private[this] val labelCols = Array("labelCol")

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

  override def getModel(directory: File): Option[Model] = {
    if (!directory.exists() || !directory.isDirectory) return None
    try {
      logger.debug(s"Directory: ${directory.getAbsolutePath}")
      val subDirs = directory.getSubDirectories

      val stagesDir = Paths.get(directory.getAbsolutePath, "stages").toFile

      val pipelineMetadata = getMetadata(directory)
      logger.debug(s"Pipeline: $pipelineMetadata")
      val stagesMetadata = stagesDir.getSubDirectories.map(getMetadata)
      logger.debug(s"Stages: $stagesMetadata")

      val model = Model(
        directory.getName,
        getRuntimeName(pipelineMetadata),
        getInputCols(stagesMetadata),
        getOutputCols(stagesMetadata)
      )
      Some(model)
    } catch {
      case e: FileNotFoundException =>
        logger.warn(s"${directory.getCanonicalPath} in not a valid SparkML model")
        None
    }
  }

  def getModels: Seq[Model] = {
    val models = sparkDir.getSubDirectories
    models.map(getModel).filter(_.isDefined).map(_.get)
  }
}




