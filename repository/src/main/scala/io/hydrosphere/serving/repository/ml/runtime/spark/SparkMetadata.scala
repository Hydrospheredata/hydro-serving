package io.prototypes.ml_repository.ml.runtime.spark

/**
  * Created by Bulat on 31.05.2017.
  */
case class SparkMetadata(
                     `class`: String,
                     timestamp: Long,
                     sparkVersion: String,
                     uid: String,
                     paramMap: Map[String, Any],
                     numFeatures: Option[Int],
                     numClasses: Option[Int],
                     numTrees: Option[Int]
                   )

object SparkMetadata {
  import io.hydrosphere.serving.repository.util.MapAnyJson._
  import spray.json._

  implicit val sparkMetadataFormat: RootJsonFormat[SparkMetadata] = jsonFormat8(SparkMetadata.apply)

  def fromJson(json: String): SparkMetadata = {
    json.parseJson.convertTo[SparkMetadata]
  }

  def extractParams(sparkMetadata: SparkMetadata, params: Seq[String]): Seq[String] = {
    params.map(sparkMetadata.paramMap.get).filter(_.isDefined).map(_.get.asInstanceOf[String])
  }
}