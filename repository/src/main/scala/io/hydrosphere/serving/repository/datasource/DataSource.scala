package io.hydrosphere.serving.repository.datasource

import java.io.File

import akka.actor.{ActorRef, Props}
import io.hydrosphere.serving.repository.datasource.local.LocalSource
import io.hydrosphere.serving.repository.datasource.s3.S3Source
import io.hydrosphere.serving.repository.repository.IndexEntry

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