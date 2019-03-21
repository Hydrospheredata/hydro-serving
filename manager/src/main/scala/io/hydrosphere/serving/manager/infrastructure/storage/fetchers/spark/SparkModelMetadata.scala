package io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark

import io.hydrosphere.serving.manager.infrastructure.protocol.CompleteJsonProtocol

case class SparkModelMetadata(
  `class`: String,
  timestamp: Long,
  sparkVersion: String,
  uid: String,
  paramMap: Map[String, Any],
  numFeatures: Option[Int],
  numClasses: Option[Int],
  numTrees: Option[Int]
) {
  def getParam[T](param: String): Option[T] = {
    paramMap.get(param).map(_.asInstanceOf[T])
  }

  def toMap: Map[String, String] = {
    val basic = Map(
      "sparkml.class" -> `class`,
      "sparkml.timestamp" -> timestamp.toString,
      "sparkml.sparkVersion" -> sparkVersion,
      "sparkml.uid" -> uid,
    )
    val opts = Map(
      "sparkml.numFeatures" -> numFeatures,
      "sparkml.numClasses" -> numClasses,
      "sparkml.numTrees" -> numTrees,
    ).collect { case (k, Some(v)) => k -> v.toString }

    basic ++ opts
  }
}

object SparkModelMetadata extends CompleteJsonProtocol {
  import spray.json._
  implicit val sparkMetadataFormat: RootJsonFormat[SparkModelMetadata] = jsonFormat8(SparkModelMetadata.apply)

  def fromJson(json: String): SparkModelMetadata = {
    json.parseJson.convertTo[SparkModelMetadata]
  }

  def extractParams(sparkMetadata: SparkModelMetadata, params: Seq[String]): Seq[String] = {
    params.map(sparkMetadata.paramMap.get).filter(_.isDefined).map(_.get.asInstanceOf[String])
  }
}
