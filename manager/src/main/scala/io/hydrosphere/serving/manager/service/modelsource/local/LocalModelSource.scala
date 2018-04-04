package io.hydrosphere.serving.manager.service.modelsource.local

import java.io.File
import java.nio.file._

import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.manager.util.FileUtils._

class LocalModelSource(val sourceDef: LocalSourceDef) extends ModelSource {
  val rootDir = sourceDef.pathPrefix.map(Paths.get(_)).getOrElse(Paths.get("/"))

  override def getReadableFile(path: String): File = {
    val requestedPath = rootDir.resolve(path)
    requestedPath.toFile
  }

  override def getSubDirs(path: String): List[String] = {
    rootDir.resolve(path)
      .toFile
      .getSubDirectories
      .map(_.getName)
      .toList
  }

  override def getAllFiles(path: String): List[String] = {
    val fullPath = rootDir.resolve(path)
    val fullUri = fullPath.toUri
    fullPath
      .toFile
      .listFilesRecursively
      .map(p => fullUri.relativize(p.toURI).toString)
      .toList
  }

  override def getAbsolutePath(modelPath: String): Path = {
    rootDir.resolve(modelPath)
  }

  override def isExist(path: String): Boolean = {
    val requestedPath = rootDir.resolve(path)
    Files.exists(requestedPath)
  }

  override def writeFile(path: String, localFile: File): Unit = {
    val destPath = getAbsolutePath(path)
    val parentPath = destPath.getParent
    if (!Files.exists(parentPath)) {
      Files.createDirectories(parentPath)
    }
    Files.copy(localFile.toPath, destPath, StandardCopyOption.REPLACE_EXISTING)
  }
}