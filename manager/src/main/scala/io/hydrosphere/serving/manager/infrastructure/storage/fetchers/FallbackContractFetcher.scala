package io.hydrosphere.serving.manager.infrastructure.storage.fetchers

import java.nio.file.Files

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.infrastructure.storage.StorageOps
import io.hydrosphere.serving.model.api.{ModelMetadata, ModelType}
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConverters._
import scala.util.Try

object FallbackContractFetcher extends ModelFetcher with Logging {
  override def fetch(source: StorageOps, directory: String): Option[ModelMetadata] = {
    getContract(source, directory).map { contract =>
      ModelMetadata(
        modelName = directory,
        modelType = ModelType.Unknown("unknown", "fallback"),
        contract = contract
      )
    }
  }

  private def getContract(source: StorageOps, modelName: String): Option[ModelContract] = {
    val txtContract = source.getReadableFile(s"$modelName/contract.prototxt")
      .toOption
      .flatMap { metaFile =>
        Try {
          val metaStr = Files.readAllLines(metaFile.toPath).asScala.mkString
          ModelContract.fromAscii(metaStr)
        }.toOption
      }
    val binContract = source.getReadableFile(s"$modelName/contract.protobin")
      .toOption
      .flatMap { metaFile =>
        Try {
          val metaBin = Files.readAllBytes(metaFile.toPath)
          ModelContract.parseFrom(metaBin)
        }.toOption
      }
    txtContract.orElse(binContract)
  }
}