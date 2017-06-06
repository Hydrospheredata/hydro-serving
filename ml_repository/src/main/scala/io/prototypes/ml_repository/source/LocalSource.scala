package io.prototypes.ml_repository.source

import java.nio.file._

/**
  * Created by Bulat on 31.05.2017.
  */
case class LocalSource(path: Path) extends ModelSource {

}

object LocalSource {
  def fromMap(map: Map[String, String]): LocalSource = {
    LocalSource(Paths.get(map("path")))
  }
}