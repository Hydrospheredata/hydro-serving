package io.hydrosphere.serving.manager.service.source.fetchers.keras

import java.nio.file.Path

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.service.source.storages.ModelStorage
import io.hydrosphere.serving.manager.util.HDF5File
import io.hydrosphere.serving.model.api.{ModelMetadata, ModelType}
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.scala.Logging

import scala.io.Source
import scala.util.control.NonFatal

private[keras] trait ModelConfigParser {
  def importModel: Option[ModelMetadata]
}

private[keras] object ModelConfigParser extends Logging {
  def importer(source: ModelStorage, directory: String): Option[ModelConfigParser] = {
    findH5file(source, directory) match {
      case Some(h5Path) =>
        logger.debug(s"Found a .h5 file: $h5Path")
        Some(ModelConfigParser.H5(h5Path))

      case None =>
        logger.debug(s"Did not find keras model files")
        None
    }
  }

  def findH5file(source: ModelStorage, directory: String): Option[Path] = {
    source.getReadableFile(directory).right.toOption.flatMap { dirFile =>
      dirFile.listFiles().find(f => f.isFile && f.getName.endsWith(".h5")).map(_.toPath)
    }
  }

  def findJsonfile(source: ModelStorage, directory: String): Option[Path] = {
    source.getReadableFile(directory).right.toOption.flatMap { dirFile =>
      dirFile.listFiles().find(f => f.isFile && f.getName.endsWith(".json")).map(_.toPath)
    }
  }

  case class H5(h5path: Path) extends ModelConfigParser {
    def importModel: Option[ModelMetadata] = {
      val h5File = HDF5File(h5path.toString)
      try {
        val modelName = FilenameUtils.removeExtension(h5path.getFileName.getFileName.toString)
        val jsonModelConfig = h5File.readAttributeAsString("model_config")
        val kerasVersion = h5File.readAttributeAsString("keras_version")
        JsonString(jsonModelConfig, modelName, kerasVersion).importModel
      } finally {
        h5File.close()
      }
    }
  }

  case class JsonFile(jsonPath: Path) extends ModelConfigParser {
    def importModel: Option[ModelMetadata] = {
      val jsonModelConfig = Source.fromFile(jsonPath.toFile).mkString
      JsonString(jsonModelConfig, jsonPath.toString, "unknown").importModel
    }
  }

  case class JsonString(modelConfigJson: String, name: String, version: String) extends ModelConfigParser {

    import spray.json._

    override def importModel: Option[ModelMetadata] = {
      try {
        val config = modelConfigJson.parseJson.convertTo[ModelConfig]
        logger.debug(s"ModelConfig class: ${config.getClass}")
        val contract = ModelContract(
          modelName = name,
          signatures = config.toSignatures
        )
        Some(
          ModelMetadata(
            modelName = name,
            modelType = ModelType.Unknown("keras", version),
            contract = contract
          )
        )
      } catch {
        case NonFatal(x) =>
          logger.debug("Error while reading ModelConfig json", x)
          None
      }
    }
  }

}