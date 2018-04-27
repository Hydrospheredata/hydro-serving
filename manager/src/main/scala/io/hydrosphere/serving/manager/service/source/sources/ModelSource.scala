package io.hydrosphere.serving.manager.service.source.sources

import java.io.File
import java.nio.file.Path

import io.hydrosphere.serving.manager.model.{HFResult, HResult}
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.{LocalSourceParams, S3SourceParams}
import io.hydrosphere.serving.manager.service.source.sources.local.{LocalModelSource, LocalSourceDef}
import io.hydrosphere.serving.manager.service.source.sources.s3.{S3ModelSource, S3SourceDef}

trait ModelSource {
  def getReadableFile(path: String): HResult[File]

  def getAllFiles(folder: String): HResult[List[String]]

  def getSubDirs(path: String): HResult[List[String]]

  def sourceDef: SourceDef

  def exists(path: String): Boolean

  def writeFile(path: String, localFile: File): HResult[Path]
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