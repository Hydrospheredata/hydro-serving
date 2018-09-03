package io.hydrosphere.serving.manager.service.source.storages

import java.io.File
import java.nio.file.Path

import io.hydrosphere.serving.model.api.HResult

trait ModelStorage {
  def getReadableFile(path: String): HResult[File]

  def getAllFiles(folder: String): HResult[List[String]]

  def getSubDirs(path: String): HResult[List[String]]

  def sourceDef: ModelStorageDefinition

  def exists(path: String): Boolean

  def writeFile(path: String, localFile: File): HResult[Path]
}
