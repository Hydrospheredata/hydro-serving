package io.prototypes.ml_repository.source

import akka.actor.ActorRef
import io.prototypes.ml_repository.watcher.SourceWatcher

/**
  * Created by Bulat on 31.05.2017.
  */
trait ModelSource{
}

object ModelSource {
  def fromMap(map: Map[String, String]): ModelSource = map("type") match {
    case "local" =>
      LocalSource.fromMap(map)
    case "hdfs" =>
      HDFSSource.fromMap(map)
    case "s3" =>
      S3Source.fromMap(map)
    case x =>
      throw new IllegalArgumentException(s"Unknown data source: $x")
  }
}