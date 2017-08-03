package io.hydrosphere.serving.manager.service.modelfetcher

import scala.collection.JavaConversions._
import java.io.FileNotFoundException
import java.nio.file.{Files, NoSuchFileException}
import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.{Model, SchematicRuntimeType}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging
import org.tensorflow.framework.{SavedModel}
/**
  * Created by bulat on 24/07/2017.
  */
object TensorflowModelFetcher extends ModelFetcher with Logging {
  override def fetch(source: ModelSource, directory: String): Option[Model] = {
    try {
      val pbFile = source.getReadableFile(s"$directory/saved_model.pb")
      val savedModel = SavedModel.parseFrom(Files.newInputStream(pbFile.toPath))
      val metagraph = savedModel.getMetaGraphs(0)
      val signature = metagraph.getSignatureDefMap.get("serving_default")
      val inputs = signature.getInputsMap.keySet.toList
      val outputs = signature.getOutputsMap.keySet.toList
      Some(
        Model(
          -1,
          directory,
          s"${source.getSourcePrefix()}:/$directory",
          Some(
            new SchematicRuntimeType("tensorflow", "1.0")
          ),
          None,
          outputs,
          inputs,
          LocalDateTime.now(),
          LocalDateTime.now()
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
