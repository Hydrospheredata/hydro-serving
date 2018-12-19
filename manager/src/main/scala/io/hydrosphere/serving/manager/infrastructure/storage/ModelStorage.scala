package io.hydrosphere.serving.manager.infrastructure.storage

import java.io.File
import java.nio.file.Path

import io.hydrosphere.serving.model.api.HResult

trait ModelStorage {
  def rootPath: Path

  def getReadableFile(path: String): HResult[File]

  def getAllFiles(folder: String): HResult[List[String]]

  def getSubDirs(path: String): HResult[List[String]]

  def exists(path: String): Boolean

  def writeFile(path: String, localFile: File): HResult[Path]
}
