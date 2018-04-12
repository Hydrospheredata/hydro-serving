package io.hydrosphere.serving.manager.service.source.sources

import java.io.File
import java.nio.file.Path

import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.{LocalSourceParams, S3SourceParams}
import io.hydrosphere.serving.manager.service.source.sources.local.{LocalModelSource, LocalSourceDef}
import io.hydrosphere.serving.manager.service.source.sources.s3.{S3ModelSource, S3SourceDef}

trait ModelSource {
  def getReadableFile(path: String): File

  def getAllFiles(folder: String): List[String]

  def getSubDirs(path: String): List[String]

  def sourceDef: SourceDef

  def getAbsolutePath(modelSource: String): Path

  def isExist(path: String): Boolean

  def writeFile(path: String, localFile: File)
}


object ModelSource {
  def fromConfig(conf: ModelSourceConfig): ModelSource = {
    val params = conf.params
    params match {
      case x: LocalSourceParams =>
        new LocalModelSource(
          LocalSourceDef.fromConfig(conf.name, x)
        )
      case x: S3SourceParams =>
        new S3ModelSource(
          S3SourceDef.fromConfig(conf.name, x)
        )
      case x =>
        throw new IllegalArgumentException(s"Unknown params: $x")
    }
  }
}