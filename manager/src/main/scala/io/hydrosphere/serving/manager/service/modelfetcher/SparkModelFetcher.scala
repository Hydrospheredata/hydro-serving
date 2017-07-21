package io.hydrosphere.serving.manager.service.modelfetcher

import java.io.FileNotFoundException
import java.nio.file.Files
import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.{Model, RuntimeType}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.model.CommonJsonSupport
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._


case class SparkMetadata(
  `class`: String,
  timestamp: Long,
  sparkVersion: String,
  uid: String,
  paramMap: Map[String, Any],
  numFeatures: Option[Int],
  numClasses: Option[Int],
  numTrees: Option[Int]
)

object SparkMetadata extends CommonJsonSupport {

  import spray.json._

  implicit val sparkMetadataFormat: RootJsonFormat[SparkMetadata] = jsonFormat8(SparkMetadata.apply)

  def fromJson(json: String): SparkMetadata = {
    json.parseJson.convertTo[SparkMetadata]
  }

  def extractParams(sparkMetadata: SparkMetadata, params: Seq[String]): Seq[String] = {
    params.map(sparkMetadata.paramMap.get).filter(_.isDefined).map(_.get.asInstanceOf[String])
  }
}

class SparkModelFetcher(val source: ModelSource) extends ModelFetcher with Logging {
  private[this] val inputCols = Array("inputCol", "featuresCol")
  private[this] val outputCols = Array("outputCol", "predictionCol", "probabilityCol", "rawPredictionCol")
  private[this] val labelCols = Array("labelCol")

  private def getRuntimeType(sparkMetadata: SparkMetadata): RuntimeType = {
    RuntimeType(-1, "spark", "1.0.0")
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

    if (labels.isEmpty) {
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
      val fullPath = s"${source.getSourcePrefix()}:/spark/$directory"
      val stagesDir = s"$fullPath/stages"

      val pipelineMetadata = getMetadata(directory)
      val stagesMetadata = source.getSubDirs(stagesDir).map { stage =>
        getStageMetadata(directory, stage)
      }

      Some(Model(
        -1,
        directory,
        fullPath,
        Some(getRuntimeType(pipelineMetadata)),
        None,
        getOutputCols(stagesMetadata),
        getInputCols(stagesMetadata),
        LocalDateTime.now(),
        LocalDateTime.now()
      ))
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
