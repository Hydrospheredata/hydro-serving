package io.hydrosphere.serving.manager.service.source.fetchers.spark

import java.nio.file.Files

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.model.api.{ModelMetadata, _}
import io.hydrosphere.serving.manager.model.{HResult, Result}
import io.hydrosphere.serving.manager.service.source.fetchers.ModelFetcher
import io.hydrosphere.serving.manager.service.source.fetchers.spark.mappers.SparkMlTypeMapper
import io.hydrosphere.serving.manager.service.source.storages.ModelStorage
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConverters._


object SparkModelFetcher extends ModelFetcher with Logging {
  private def getStageMetadata(source: ModelStorage, model: String, stage: String): HResult[SparkModelMetadata] = {
    getMetadata(source, s"$model/stages/$stage")
  }

  private def getMetadata(source: ModelStorage, model: String): HResult[SparkModelMetadata] = {
    source.getReadableFile(s"$model/metadata/part-00000") match {
      case Left(error) =>
        logger.debug(s"'$model/metadata/part-00000' in '${source.sourceDef.name}' doesn't exist")
        Result.error(error)
      case Right(metaFile) =>
        val metaStr = Files.readAllLines(metaFile.toPath).asScala.mkString
        Result.ok(SparkModelMetadata.fromJson(metaStr))
    }
  }

  override def fetch(source: ModelStorage, directory: String): Option[ModelMetadata] = {
    val stagesDir = s"$directory/stages"

    getMetadata(source, directory) match {
      case Left(error) =>
        logger.warn(s"Unexpected fetch error: $error")
        None
      case Right(pipelineMetadata) =>
        processPipeline(source, directory, stagesDir, pipelineMetadata)
    }
  }

  private def processPipeline(source: ModelStorage, directory: String, stagesDir: String, pipelineMetadata: SparkModelMetadata) = {
    source.getSubDirs(stagesDir) match {
      case Left(error) =>
        logger.warn(s"Unexpected fetch error: $error")
        None
      case Right(stages) =>
        val sM = Result.traverse(stages) { stage =>
          getStageMetadata(source, directory, stage)
        }
        sM match {
          case Left(error) =>
            logger.warn(s"Unexpected fetch error: $error")
            None
          case Right(stagesMetadata) =>
            Some(
              ModelMetadata(
                modelName = directory,
                modelType = ModelType.Spark(pipelineMetadata.sparkVersion),
                contract = ModelContract(
                  directory,
                  Seq(processStages(stagesMetadata))
                )
              )
            )
        }
    }
  }

  private def processStages(stagesMetadata: Seq[SparkModelMetadata]) = {
    val mappers = stagesMetadata.map(SparkMlTypeMapper.apply)
    val inputs = mappers.map(_.inputSchema)
    val outputs = mappers.map(_.outputSchema)
    val labels = mappers.flatMap(_.labelSchema)

    val allLabels = labels.map(_.name)
    val allIns = inputs.flatten.map(x => x.name -> x).toMap
    val allOuts = outputs.flatten.map(x => x.name -> x).toMap

    val inputSchema = if (allLabels.isEmpty) {
      (allIns -- allOuts.keys).map { case (_, y) => y }.toList
    } else {
      val trainInputs = stagesMetadata
        .filter { stage =>
          val mapper = SparkMlTypeMapper(stage)
          mapper.outputSchema.map(_.name).containsSlice(allLabels)
        }
        .map { stage =>
          SparkMlTypeMapper(stage).inputSchema
        }
      val allTrains = trainInputs.flatten.map(x => x.name -> x).toMap
      (allIns -- allOuts.keys -- allTrains.keys).map { case (_, y) => y }.toList
    }
    val outputSchema = (allOuts -- allIns.keys).map { case (_, y) => y }.toList

    val signature = ModelSignature("default_spark", inputSchema, outputSchema)
    signature
  }
}



