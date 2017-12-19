package io.hydrosphere.serving.manager.service.modelfetcher

import java.nio.file.Files

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.model_api._
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._

/**
  * Created by Bulat on 31.05.2017.
  */
object ScikitModelFetcher extends ModelFetcher with Logging {
  override def fetch(source: ModelSource, directory: String): Option[ModelMetadata] = {
    if (source.isExist(s"$directory/model.pkl")) {
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
    if (source.isExist(s"$modelName/metadata.prototxt")) {
      val metaFile = source.getReadableFile(s"$modelName/metadata.prototxt")
      val metaStr = Files.readAllLines(metaFile.toPath).mkString
      ModelContract.fromAscii(metaStr)
    } else {
      ModelContract()
    }
  }
}