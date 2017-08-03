package io.hydrosphere.serving.manager.service.modelfetcher

import java.io.FileNotFoundException
import java.nio.file.{Files, NoSuchFileException}
import java.time.LocalDateTime

import io.hydrosphere.serving.manager.model.{Model, RuntimeType}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.model.CommonJsonSupport
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConversions._

case class ScikitMetadata(
  model: String,
  inputs: List[String],
  outputs: List[String]
)

object ScikitMetadata extends CommonJsonSupport{
  import spray.json._

  implicit val scikitMetadataFormat: RootJsonFormat[ScikitMetadata] = jsonFormat3(ScikitMetadata.apply)

  def fromJson(json: String): ScikitMetadata = {
    json.parseJson.convertTo[ScikitMetadata]
  }
}

/**
  * Created by Bulat on 31.05.2017.
  */
object ScikitModelFetcher extends ModelFetcher with Logging {

  private def getMetadata(source: ModelSource, modelName: String): ScikitMetadata = {
    val metaFile = source.getReadableFile(s"$modelName/metadata.json")
    val metaStr = Files.readAllLines(metaFile.toPath).mkString
    ScikitMetadata.fromJson(metaStr)
  }

  override def fetch(source: ModelSource, directory: String): Option[Model] = {
    try {
      val metadata = getMetadata(source, directory)
      Some(Model(
        -1,
        directory,
        s"${source.getSourcePrefix()}:/$directory",
        None,
        None,
        metadata.outputs,
        metadata.inputs,
        LocalDateTime.now(),
        LocalDateTime.now()
      ))
    } catch {
      case e: NoSuchFileException =>
        logger.debug(s"$directory in not a valid SKLearn model")
        None
      case e: FileNotFoundException =>
        logger.debug(s"$directory in not a valid SKLearn model")
        None
    }
  }
}