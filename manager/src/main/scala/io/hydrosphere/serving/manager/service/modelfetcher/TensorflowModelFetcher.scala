package io.hydrosphere.serving.manager.service.modelfetcher

import java.io.FileNotFoundException
import java.nio.file.{Files, NoSuchFileException}

import io.hydrosphere.serving.manager.model.SchematicRuntimeType
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging
import org.tensorflow.framework.SavedModel

import scala.collection.JavaConversions._
/**
  * Created by bulat on 24/07/2017.
  */
object TensorflowModelFetcher extends ModelFetcher with Logging {
  override def fetch(source: ModelSource, directory: String): Option[ModelMetadata] = {
    try {
      val pbFile = source.getReadableFile(s"$directory/saved_model.pb")
      val savedModel = SavedModel.parseFrom(Files.newInputStream(pbFile.toPath))
      val metagraph = savedModel.getMetaGraphs(0)
      val signature = metagraph.getSignatureDefMap.get("serving_default")
      val inputs = signature.getInputsMap.keySet.toList
      val outputs = signature.getOutputsMap.keySet.toList
      Some(
        ModelMetadata(
          directory,
          Some(new SchematicRuntimeType("hydro-serving/runtime-tensorflow", "0.0.1")),
          outputs,
          inputs
        )
      )
    } catch {
      case e: NoSuchFileException =>
        logger.debug(s"$directory in not a valid Tensorflow model")
        None
      case e: FileNotFoundException =>
        logger.debug(s"$source $directory in not a valid Tensorflow model")
        None
    }
  }
}
