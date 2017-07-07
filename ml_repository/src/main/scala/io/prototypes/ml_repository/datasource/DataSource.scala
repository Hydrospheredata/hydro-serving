package io.prototypes.ml_repository.datasource

import java.io.File

import akka.actor.{ActorRef, Props}
import io.prototypes.ml_repository.datasource.local.LocalSource
import io.prototypes.ml_repository.datasource.s3.S3Source
import io.prototypes.ml_repository.ml.Model
import io.prototypes.ml_repository.repository.IndexEntry

/**
  * Created by Bulat on 31.05.2017.
  */
trait DataSource {
  def getReadableFile(runtimeName: String, modelName: String, path: String): File
  def getAllFiles(runtimeName: String, modelName: String): List[String]
  def getSubDirs(path: String): List[String]
  def getSubDirs: List[String]

  def watcherProps(indexer: ActorRef): Props
  def onNewIndexEntries(idx: Seq[IndexEntry])
}

object DataSource {
  def fromMap(map: Map[String, String]): DataSource = map("type") match {
    case "local" =>
      LocalSource.fromMap(map)
    case "s3" =>
      S3Source.fromMap(map)
    case x =>
      throw new IllegalArgumentException(s"Unknown data source: $x")
  }
}