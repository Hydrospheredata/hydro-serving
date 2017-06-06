package io.prototypes.ml_repository.runtime.spark

import java.io.File
import java.nio.file.Paths

import io.prototypes.ml_repository.Model
import io.prototypes.ml_repository.runtime.MLRuntime
import io.prototypes.ml_repository.utils.FileUtils._

import scala.io.Source

/**
  * Created by Bulat on 31.05.2017.
  */
class SparkRuntime(val sparkDir: File) extends MLRuntime {
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

  private val inputCols = Array("inputCol", "featuresCol")
  private val outputCols = Array("outputCol", "predictionCol", "probabilityCol", "rawPredictionCol")
  private val labelCols = Array("labelCol")

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
    println(s"Directory: ${directory.getAbsolutePath}")
    val subDirs = directory.getSubDirectories

    val stagesDir = Paths.get(directory.getAbsolutePath, "stages").toFile

    val pipelineMetadata = getMetadata(directory)
    println(s"Pipeline: $pipelineMetadata")
    val stagesMetadata = stagesDir.getSubDirectories.map(getMetadata)
    println(s"Stages: $stagesMetadata")

    val model = Model(
      directory.getName,
      getRuntimeName(pipelineMetadata),
      getInputCols(stagesMetadata),
      getOutputCols(stagesMetadata)
    )
    Some(model)
  }

  def getModels: Seq[Model] = {
    val models = sparkDir.getSubDirectories
    models.map(getModel).filter(_.isDefined).map(_.get)
  }
}




