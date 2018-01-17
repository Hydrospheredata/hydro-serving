package io.hydrosphere.serving.manager.service.modelfetcher.spark

import java.io.FileNotFoundException
import java.nio.file.{Files, NoSuchFileException}

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.SchematicRuntime
import io.hydrosphere.serving.manager.model.api.ModelMetadata
import io.hydrosphere.serving.manager.service.modelfetcher.ModelFetcher
import io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers.SparkMlTypeMapper
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.manager.model.Runtime
import io.hydrosphere.serving.manager.model.api._
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._


object SparkModelFetcher extends ModelFetcher with Logging {
  private def getStageMetadata(source: ModelSource, model: String, stage: String): SparkModelMetadata = {
    getMetadata(source, s"$model/stages/$stage")
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
      val mappers = stagesMetadata.map(SparkMlTypeMapper.apply)
      val inputs = mappers.map(_.inputSchema)
      val outputs = mappers.map(_.outputSchema)
      val labels = mappers.map(_.labelSchema).filter(_.isDefined).map(_.get)

      val allLabels = labels.flatten.map(_.fieldName)
      val allIns = inputs.flatten.map(x => x.fieldName -> x).toMap
      val allOuts = outputs.flatten.map(x => x.fieldName -> x).toMap

      val inputSchema = if (allLabels.isEmpty) {
        (allIns -- allOuts.keys).map{case (_,y) => y}.toList
      } else {
        val trainInputs = stagesMetadata.filter { stage =>
          val mapper = SparkMlTypeMapper(stage)
          val outs = mapper.outputSchema
          outs.map(x => x.fieldName -> x).toMap.keys.containsAll(allLabels)
        }.map{ stage =>
          SparkMlTypeMapper(stage).inputSchema
        }
        val allTrains = trainInputs.flatten.map(x => x.fieldName -> x).toMap
        (allIns -- allOuts.keys -- allTrains.keys).map{case (_,y) => y}.toList
      }
      val outputSchema = (allOuts -- allIns.keys).map{case (_,y) => y}.toList

      val signature = ModelSignature("default_spark", inputSchema, outputSchema)

      val contract = ModelContract(
        directory,
        List(signature)
      )

      Some(
        ModelMetadata(
          modelName = directory,
          modelType = ModelType.Spark(pipelineMetadata.sparkVersion),
          contract = contract
        )
      )
    } catch {
      case _: NoSuchFileException =>
        logger.debug(s"$directory in not a valid SparkML model")
        None
      case _: FileNotFoundException =>
        logger.debug(s"$source $directory in not a valid SparkML model")
        None
    }
  }
}

