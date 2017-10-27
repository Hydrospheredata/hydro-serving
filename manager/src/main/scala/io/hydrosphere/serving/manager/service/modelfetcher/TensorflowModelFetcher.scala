package io.hydrosphere.serving.manager.service.modelfetcher

import java.io.FileNotFoundException
import java.nio.file.{Files, NoSuchFileException}

import io.hydrosphere.serving.manager.model.SchematicRuntimeType
import io.hydrosphere.serving.model_api._
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import org.apache.logging.log4j.scala.Logging
import org.tensorflow.framework.{DataType, SavedModel, TensorInfo}

import scala.collection.JavaConversions._
/**
  * Created by bulat on 24/07/2017.
  */
object TensorflowModelFetcher extends ModelFetcher with Logging {
  def getTypeFields(fields: Map[String, TensorInfo]): List[ModelField] = {
    fields.map{
      case (name, tensorInfo) =>
        val tensorShape = tensorInfo.getTensorShape
        val fieldType: ScalarField = tensorInfo.getDtype match {
          case DataType.DT_FLOAT| DataType.DT_DOUBLE => FDouble // TODO Float support
          case DataType.DT_STRING => FString
          case _ => FInteger
        }
        val fullType = if (tensorShape.getDimCount == 0) { // rank-0 tensor = scalar
          fieldType
        } else {
          FMatrix(
            fieldType,
            tensorShape.getDimList.map(_.getSize).toList
          )
        }
        ModelField(name, fullType)
    }.toList
  }

  override def fetch(source: ModelSource, directory: String): Option[ModelMetadata] = {
    try {
      val pbFile = source.getReadableFile(s"$directory/saved_model.pb")
      val savedModel = SavedModel.parseFrom(Files.newInputStream(pbFile.toPath))
      val metagraph = savedModel.getMetaGraphs(0)
      val signature = metagraph.getSignatureDefMap.get("serving_default")
      val inputs = getTypeFields(signature.getInputsMap.toMap)
      val outputs = getTypeFields(signature.getOutputsMap.toMap)
      Some(
        ModelMetadata(
          directory,
          Some(new SchematicRuntimeType("hydrosphere/serving-runtime-tensorflow", "0.0.1")),
          DataFrame(outputs),
          DataFrame(inputs)
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
