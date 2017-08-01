package io.hydrosphere.serving.manager.service.modelsource

import java.io.File
import java.nio.file._

import io.hydrosphere.serving.manager.LocalModelSourceConfiguration
import io.hydrosphere.serving.util.FileUtils._

/**
  *
  */
class LocalModelSource(val conf: LocalModelSourceConfiguration) extends ModelSource {
  val sourceFile = new File(conf.path.toString)

  override def getReadableFile(runtimeName: String, modelName: String, path: String): File = {
    val requestedPath = Paths.get(conf.path.toString, runtimeName, modelName, path)
    requestedPath.toFile
  }

  override def getSubDirs(path: String): List[String] = {
    val fullPath = Paths.get(conf.path.toString, path)
    fullPath.toFile.getSubDirectories
      .map(_.getName).toList
  }

  override def getSubDirs: List[String] = {
    sourceFile.getSubDirectories.map(_.getName).toList
  }

  override def getAllFiles(runtimeName: String, modelName: String): List[String] = {
    val fullPath = Paths.get(conf.path.toString, runtimeName, modelName)
    val fullUri = fullPath.toUri
    fullPath.toFile.listFilesRecursively.map(p => fullUri.relativize(p.toURI).toString)
  }

  override def getSourcePrefix(): String = conf.name

  override def getLocalCopy(source: String): Path =
    Paths.get(conf.path, source.split(":").last)
}