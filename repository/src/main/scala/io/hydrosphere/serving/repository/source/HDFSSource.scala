package io.hydrosphere.serving.repository.source

/**
  * Created by Bulat on 31.05.2017.
  */
case class HDFSSource(path: String) extends ModelSource {
}

object HDFSSource {
  def fromMap(map: Map[String, String]): HDFSSource = {
    HDFSSource(map("path"))
  }
}