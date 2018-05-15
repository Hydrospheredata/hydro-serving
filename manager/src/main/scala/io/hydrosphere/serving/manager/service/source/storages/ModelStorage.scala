package io.hydrosphere.serving.manager.service.source.storages

import java.io.File
import java.nio.file.Path

import io.hydrosphere.serving.manager.model.{HFResult, HResult}
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.{LocalSourceParams, S3SourceParams}
import io.hydrosphere.serving.manager.service.source.storages.local.{LocalModelStorage, LocalModelStorageDefinition}
import io.hydrosphere.serving.manager.service.source.storages.s3.{S3ModelStorage, S3ModelStorageDefinition}

trait ModelStorage {
  def getReadableFile(path: String): HResult[File]

  def getAllFiles(folder: String): HResult[List[String]]

  def getSubDirs(path: String): HResult[List[String]]

  def sourceDef: ModelStorageDefinition

  def exists(path: String): Boolean

  def writeFile(path: String, localFile: File): HResult[Path]
}

object ModelStorage {
  def fromConfig(conf: ModelSourceConfig): ModelStorage = {
    val params = conf.params
    params match {
      case x: LocalSourceParams =>
        new LocalModelStorage(
          LocalModelStorageDefinition.fromConfig(conf.name, x)
        )
      case x: S3SourceParams =>
        new S3ModelStorage(
          S3ModelStorageDefinition.fromConfig(conf.name, x)
        )
      case x =>
        throw new IllegalArgumentException(s"Unknown params: $x")
    }
  }
}