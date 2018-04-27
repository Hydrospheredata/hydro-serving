package io.hydrosphere.serving.manager.service.source.fetchers

import java.nio.file.Files

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.model.api.ModelMetadata
import io.hydrosphere.serving.manager.model.api._
import io.hydrosphere.serving.manager.service.source.sources.ModelSource
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._

object ScikitModelFetcher extends ModelFetcher with Logging {
  override def fetch(source: ModelSource, directory: String): Option[ModelMetadata] = {
    if (source.exists(s"$directory/model.pkl")) {
      val contract = getContract(source, directory)
      Some(
        ModelMetadata(
          modelName = directory,
          modelType = ModelType.Scikit(),
          contract = contract
        )
      )
    } else {
      None
    }
  }

  private def getContract(source: ModelSource, modelName: String): ModelContract = {
    val fileResult = source.getReadableFile(s"$modelName/metadata.prototxt")
    if (fileResult.isRight) {
      val metaFile = fileResult.right.get
      val metaStr = Files.readAllLines(metaFile.toPath).mkString
      ModelContract.fromAscii(metaStr)
    } else {
      ModelContract()
    }
  }
}