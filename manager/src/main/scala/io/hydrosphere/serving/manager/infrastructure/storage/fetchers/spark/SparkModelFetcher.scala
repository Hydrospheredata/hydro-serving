package io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark

import java.nio.file.Path

import cats.data.OptionT
import cats.instances.list._
import cats.{Monad, Traverse}
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.manager.infrastructure.storage.StorageOps
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark.mappers.SparkMlTypeMapper
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.{FetcherResult, ModelFetcher}
import org.apache.logging.log4j.scala.Logging

import scala.util.Try


class SparkModelFetcher[F[_]: Monad](storageOps: StorageOps[F]) extends ModelFetcher[F] with Logging {
  private def getStageMetadata(stagesPath: Path, stage: String) = {
    getMetadata(stagesPath.resolve(stage))
  }

  private def getMetadata(model: Path) = {
    for {
      lines <- OptionT(storageOps.readText(model.resolve("metadata/part-00000")))
      text = lines.mkString
      metadata <- OptionT.fromOption(Try(SparkModelMetadata.fromJson(text)).toOption)(Monad[F])
    } yield metadata
  }

  override def fetch(directory: Path): F[Option[FetcherResult]] = {
    val modelName = directory.getFileName.toString
    val f = for {
      metadata <- getMetadata(directory)
      signature <- processPipeline(directory.resolve("stages"), metadata)
    } yield FetcherResult(
      modelName = modelName,
      modelContract = ModelContract(modelName, signature),
      metadata = metadata.toMap
    )
    f.value
  }

  private def processPipeline(stagesDir: Path, pipelineMetadata: SparkModelMetadata) = {
    for {
      stages <- OptionT(storageOps.getSubDirs(stagesDir))
      sM <- Traverse[List].traverse(stages)(x => getStageMetadata(stagesDir, x))
      signature <- OptionT.fromOption(Try(SparkModelFetcher.processStages(sM)).toOption)(Monad[F])
    } yield Seq(signature)
  }
}

object SparkModelFetcher {
  def processStages(stagesMetadata: Seq[SparkModelMetadata]): ModelSignature = {
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


