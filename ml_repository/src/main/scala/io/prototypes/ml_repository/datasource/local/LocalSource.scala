package io.prototypes.ml_repository.datasource.local

import java.io.File
import java.nio.file._

import akka.actor.{ActorRef, Props}
import io.prototypes.ml_repository.datasource.DataSource
import io.prototypes.ml_repository.repository.IndexEntry
import io.prototypes.ml_repository.utils.FileUtils._

/**
  * Created by Bulat on 31.05.2017.
  */
class LocalSource(val rootPath: Path) extends DataSource {
  val sourceFile = new File(rootPath.toString)

  override def getReadableFile(runtimeName: String, modelName: String, path: String): File = {
    val requestedPath = Paths.get(rootPath.toString, runtimeName, modelName, path)
    requestedPath.toFile
  }

  override def getSubDirs(path: String): List[String] = {
    val fullPath = Paths.get(rootPath.toString, path)
    fullPath.toFile.getSubDirectories
      .map(_.getName).toList
  }

  override def getSubDirs: List[String] = {
    sourceFile.getSubDirectories.map(_.getName).toList
  }

  override def getAllFiles(runtimeName: String, modelName: String): List[String] = {
    val fullPath = Paths.get(rootPath.toString, runtimeName, modelName)
    val fullUri = fullPath.toUri
    fullPath.toFile.listFilesRecursively.map(p => fullUri.relativize(p.toURI).toString)
  }

  override def watcherProps(indexer: ActorRef): Props = LocalSourceWatcher.props(this, indexer)

  override def onNewIndexEntries(idx: Seq[IndexEntry]): Unit = {}
}

object LocalSource {
  def fromMap(map: Map[String, String]): LocalSource = {
    new LocalSource(Paths.get(map("path")))
  }
}