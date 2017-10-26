package io.hydrosphere.serving.manager.service.modelfetcher.spark

import java.io.FileNotFoundException
import java.nio.file.{Files, NoSuchFileException}

import io.hydrosphere.serving.manager.model.SchematicRuntimeType
import io.hydrosphere.serving.manager.service.modelfetcher.ModelFetcher
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.model.RuntimeType
import io.hydrosphere.serving.model_api._
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._


object SparkModelFetcher extends ModelFetcher with Logging {
  private def getRuntimeType(sparkMetadata: SparkModelMetadata): RuntimeType = {
    new SchematicRuntimeType("hydrosphere/serving-runtime-sparklocal", "0.0.1")
  }

  private def getStageMetadata(source: ModelSource, model: String, stage: String): SparkModelMetadata = {
    val metaFile = source.getReadableFile(s"$model/stages/$stage/metadata/part-00000")
    val metaStr = Files.readAllLines(metaFile.toPath).mkString
    SparkModelMetadata.fromJson(metaStr)
  }

  private def getMetadata(source: ModelSource, model: String): SparkModelMetadata = {
    val metaFile = source.getReadableFile(s"$model/metadata/part-00000")
    val metaStr = Files.readAllLines(metaFile.toPath).mkString
    SparkModelMetadata.fromJson(metaStr)
  }

  override def fetch(source: ModelSource, directory: String): Option[ModelMetadata] = {
    try {
      val stagesDir = s"$directory/stages"

      val pipelineMetadata = getMetadata(source, directory)
      val stagesMetadata = source.getSubDirs(stagesDir).map { stage =>
        getStageMetadata(source, directory, stage)
      }

      val inputs = stagesMetadata.map(SparkMlTypeMapper.getInputSchema)
      val outputs = stagesMetadata.map(SparkMlTypeMapper.getOutputSchema)
      val labels = stagesMetadata.map(SparkMlTypeMapper.getLabels).filter(_.isDefined).map(_.get)

      val allLabels = labels.flatMap(_.definition.map(x => x.name))
      val allIns = inputs.flatMap(_.definition.map(x => x.name -> x.fieldType)).toMap
      val allOuts = outputs.flatMap(_.definition.map(x => x.name -> x.fieldType)).toMap

      val inputSchema = if (allLabels.isEmpty) {
        DataFrame((allIns -- allOuts.keys).map{case (x,y) => ModelField(x, y)}.toList)
      } else {
        val trainInputs = stagesMetadata.filter { stage =>
          val outs = SparkMlTypeMapper.getOutputSchema(stage)
          outs.definition.map(x => x.name -> x.fieldType).toMap.keys.containsAll(allLabels)
        }.map(SparkMlTypeMapper.getInputSchema)
        val allTrains = trainInputs.flatMap(_.definition.map(x => x.name -> x.fieldType)).toMap
        DataFrame((allIns -- allOuts.keys -- allTrains.keys).map{case (x,y) => ModelField(x, y)}.toList)
      }
      val outputSchema = DataFrame((allOuts -- allIns.keys).map{case (x,y) => ModelField(x, y)}.toList)

      Some(
        ModelMetadata(
          directory,
          Some(getRuntimeType(pipelineMetadata)),
          outputSchema,
          inputSchema
        )
      )
    } catch {
      case e: NoSuchFileException =>
        logger.debug(s"$directory in not a valid SparkML model")
        None
      case e: FileNotFoundException =>
        logger.debug(s"$source $directory in not a valid SparkML model")
        None
    }
  }
}

