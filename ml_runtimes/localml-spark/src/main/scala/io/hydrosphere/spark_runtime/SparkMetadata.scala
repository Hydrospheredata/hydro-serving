package io.hydrosphere.spark_runtime

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by Bulat on 05.06.2017.
  */
case class SparkMetadata(name: String,
                         runtime: String,
                         inputs: List[String],
                         outputs: List[String]
                        )

object SparkMetadata extends DefaultJsonProtocol {
  implicit val metadataFormat: RootJsonFormat[SparkMetadata] = jsonFormat4(SparkMetadata.apply)
}
