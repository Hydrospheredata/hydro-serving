package io.hydrosphere.serving.manager.service.modelsource

import java.io.File
import java.nio.file._

import io.hydrosphere.serving.manager.LocalModelSourceConfiguration
import io.hydrosphere.serving.util.FileUtils._

/**
  *
  */
class LocalModelSource(val configuration: LocalModelSourceConfiguration) extends ModelSource {
  val sourceFile = new File(configuration.path.toString)

  override def getReadableFile(path: String): File = {
    val requestedPath = Paths.get(configuration.path.toString, path)
    requestedPath.toFile
  }

  override def getSubDirs(path: String): List[String] = {
    val fullPath = Paths.get(configuration.path.toString, path)
    fullPath.toFile.getSubDirectories
      .map(_.getName)
  }

  override def getSubDirs: List[String] = {
    sourceFile.getSubDirectories.map(_.getName)
  }

  override def getAllFiles(modelName: String): List[String] = {
    val fullPath = Paths.get(configuration.path.toString, modelName)
    val fullUri = fullPath.toUri
    fullPath.toFile.listFilesRecursively.map(p => fullUri.relativize(p.toURI).toString)
  }

  override def getSourcePrefix(): String = configuration.name

  override def getAbsolutePath(modelPath: String): Path = {
    Paths.get(configuration.path, modelPath)
  }

  override def isExist(path: String): Boolean = {
    val requestedPath = Paths.get(configuration.path.toString, path)
    Files.exists(requestedPath)
  }
}