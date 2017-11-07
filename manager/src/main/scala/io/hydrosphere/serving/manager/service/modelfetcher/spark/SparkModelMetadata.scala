package io.hydrosphere.serving.manager.service.modelfetcher.spark

import io.hydrosphere.serving.model.CommonJsonSupport

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
}

object SparkModelMetadata extends CommonJsonSupport {
  import spray.json._
  implicit val sparkMetadataFormat: RootJsonFormat[SparkModelMetadata] = jsonFormat8(SparkModelMetadata.apply)

  def fromJson(json: String): SparkModelMetadata = {
    json.parseJson.convertTo[SparkModelMetadata]
  }

  def extractParams(sparkMetadata: SparkModelMetadata, params: Seq[String]): Seq[String] = {
    params.map(sparkMetadata.paramMap.get).filter(_.isDefined).map(_.get.asInstanceOf[String])
  }
}
