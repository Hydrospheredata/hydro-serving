package io.hydrosphere.serving.repository.datasource.s3

import java.io.File
import java.net.URI
import java.nio.file.{Files, Paths, StandardCopyOption}

import akka.actor.{ActorRef, Props}
import awscala.Region
import awscala.s3.S3
import io.hydrosphere.serving.repository.datasource.DataSource
import io.hydrosphere.serving.repository.repository.IndexEntry

import scala.collection.concurrent.TrieMap

/**
  * Created by Bulat on 31.05.2017.
  */
class S3Source(val region: Region, val bucket: String, val queue: String) extends DataSource {
  private[this] val fileCache = TrieMap.empty[String, TrieMap[String, File]]
  private[this] implicit val s3 = S3.at(region)
  private[this] val bucketObj = s3.bucket(bucket).get

  private def downloadObject(objectPath: String): File = {
    val fileStream = bucketObj.getObject(objectPath).get.content
    val file = Files.createTempFile("s3cache-", objectPath.split("/").last)
    Files.copy(fileStream, file, StandardCopyOption.REPLACE_EXISTING)
    file.toFile
  }

  override def getSubDirs(path: String): List[String] = {
    val pUri = Paths.get(path).toUri
    val r = bucketObj.keys(path)
      .map(Paths.get(_).toUri)
      .map(k => pUri.relativize(k).toString.split("/").head)
      .distinct.toList
    r
  }

  override def getSubDirs: List[String] = {
    val r = bucketObj.keys().map(_.split("/").head).distinct.toList
    r
  }

  override def getAllFiles(runtimeName: String, modelName: String): List[String] = {
    val pUri = URI.create(s"$runtimeName/$modelName")
    val modelKeys = bucketObj.keys(pUri.toString).toList
    println(s"$pUri: $modelKeys")
    modelKeys
      .map(k =>  URI.create(k))
      .map(k => pUri.relativize(k))
      .map(k => getReadableFile(runtimeName, modelName, k.toString))

    fileCache(modelName).keys
      .map(k =>  URI.create(k))
      .map(k => pUri.relativize(k))
        .map( k => k)
      .map(_.toString)
      .toList
  }

  override def getReadableFile(runtimeName: String, modelName: String, path: String): File = {
    val fullObjectPath = URI.create(s"$runtimeName/$modelName/$path")
    println(fullObjectPath)
    val subCache = fileCache.getOrElseUpdate(modelName, TrieMap.empty)
    subCache.getOrElseUpdate(path, downloadObject(fullObjectPath.toString))
  }

  override def watcherProps(indexer: ActorRef): Props = S3SourceWatcher.props(this, indexer)

  override def onNewIndexEntries(idx: Seq[IndexEntry]): Unit = {
    idx.foreach { index =>
      fileCache.get(index.model.name).foreach { files =>
        files.values.foreach(_.delete())
      }
      fileCache += index.model.name -> TrieMap.empty
    }
  }
}

object S3Source {
  def fromMap(map: Map[String, String]): S3Source = {
      new S3Source(
        Region(map("region")),
        map("bucket"),
        map("queue")
      )
  }
}