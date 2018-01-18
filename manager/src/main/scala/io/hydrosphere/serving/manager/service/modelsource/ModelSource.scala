package io.hydrosphere.serving.manager.service.modelsource

import java.io.File
import java.nio.file.Path

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.service.modelsource.local.{LocalModelSource, LocalSourceDef}
import io.hydrosphere.serving.manager.service.modelsource.s3.{S3ModelSource, S3SourceDef}

/**
  *
  */
trait ModelSource {
  def getReadableFile(path: String): File

  def getAllFiles(folder: String): List[String]

  def getSubDirs(path: String): List[String]

  def getSubDirs: List[String]

  def sourceDef: SourceDef

  def getAbsolutePath(modelSource: String): Path

  def isExist(path: String): Boolean
}


object ModelSource {
  def fromConfig(conf: ModelSourceConfigAux): ModelSource = {
    val params = conf.params
    params match {
      case _: LocalSourceParams =>
        new LocalModelSource(
          LocalSourceDef.fromConfig(
            conf.toTyped[LocalSourceParams]
          )
        )
      case _: S3SourceParams =>
        new S3ModelSource(
          S3SourceDef.fromConfig(
            conf.toTyped[S3SourceParams]
          )
        )
      case x =>
        throw new IllegalArgumentException(s"Unknown params: $x")
    }
  }
}