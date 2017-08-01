package io.hydrosphere.serving.manager.service.modelsource

import java.io.File
import java.nio.file.Paths

import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, ModelSourceConfiguration, S3ModelSourceConfiguration}

/**
  * Created by Bulat on 31.05.2017.
  */
trait ModelSource {

  def getReadableFile(runtimeName: String, modelName: String, path: String): File

  def getAllFiles(runtimeName: String, modelName: String): List[String]

  def getSubDirs(path: String): List[String]

  def getSubDirs: List[String]

  def getSourcePrefix():String
}


object ModelSource {
  def fromConfig(conf: ModelSourceConfiguration): ModelSource = conf match {
    case local: LocalModelSourceConfiguration =>
      new LocalModelSource(local)
    case s3: S3ModelSourceConfiguration =>
      new S3ModelSource(s3)
    case x =>
      throw new IllegalArgumentException(s"Unknown data source: $x")
  }
}