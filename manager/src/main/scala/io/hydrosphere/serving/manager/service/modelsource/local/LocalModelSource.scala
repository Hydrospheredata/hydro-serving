package io.hydrosphere.serving.manager.service.modelsource.local

import java.io.File
import java.nio.file._

import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.util.FileUtils._

/**
  *
  */
class LocalModelSource(val sourceDef: LocalSourceDef) extends ModelSource {
  val sourceFile = new File(sourceDef.path.toString)

  override def getReadableFile(path: String): File = {
    val requestedPath = Paths.get(sourceDef.path.toString, path)
    requestedPath.toFile
  }

  override def getSubDirs(path: String): List[String] = {
    Paths.get(sourceDef.path.toString, path)
      .toFile
      .getSubDirectories
      .map(_.getName)
      .toList
  }

  override def getSubDirs: List[String] = {
    sourceFile
      .getSubDirectories
      .map(_.getName)
      .toList
  }

  override def getAllFiles(modelName: String): List[String] = {
    val fullPath = Paths.get(sourceDef.path.toString, modelName)
    val fullUri = fullPath.toUri
    fullPath
      .toFile
      .listFilesRecursively
      .map(p => fullUri.relativize(p.toURI).toString)
      .toList
  }

  override def getAbsolutePath(modelPath: String): Path = {
    Paths.get(sourceDef.path, modelPath)
  }

  override def isExist(path: String): Boolean = {
    val requestedPath = Paths.get(sourceDef.path.toString, path)
    Files.exists(requestedPath)
  }
}