package io.hydrosphere.serving.repository.runtime.scikit

import java.io._
import java.nio.file.Paths
import java.nio.file.Files

import io.hydrosphere.serving.repository.Model
import io.hydrosphere.serving.repository.runtime.MLRuntime
import io.hydrosphere.serving.repository.utils.FileUtils._
import org.apache.logging.log4j.scala.Logging

import scala.io.Source

/**
  * Created by Bulat on 31.05.2017.
  */
class ScikitRuntime(val scikitDir: File) extends MLRuntime with Logging {
  private def getMetadata(directory: File): ScikitMetadata = {
    val metadataPath = Paths.get(directory.getAbsolutePath, "metadata.json")
    val metadataSrc = Source.fromFile(metadataPath.toString)
    val metadataStr = try metadataSrc.getLines mkString "/n" finally metadataSrc.close()
    ScikitMetadata.fromJson(metadataStr)
  }

  override def getModel(directory: File): Option[Model] = {
    try {
      logger.debug(s"Directory: ${directory.getAbsolutePath}")
      val subDirs = directory.getSubDirectories

      val metadata = getMetadata(directory)

      val model = Model(directory.getName, "scikit", metadata.inputs, metadata.outputs)
      Some(model)
    } catch {
      case e: FileNotFoundException =>
        logger.warn(s"${directory.getCanonicalPath} in not a valid SKLearn model")
        None
    }
  }

  override def getModels: Seq[Model] = {
    val models = scikitDir.getSubDirectories
    models.map(getModel).filter(_.isDefined).map(_.get)
  }
}
