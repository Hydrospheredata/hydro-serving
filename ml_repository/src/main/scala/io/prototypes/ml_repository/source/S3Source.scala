package io.prototypes.ml_repository.source

/**
  * Created by Bulat on 31.05.2017.
  */
case class S3Source() extends ModelSource {
}

object S3Source {
  def fromMap(map: Map[String, String]) = {
      S3Source()
  }
}