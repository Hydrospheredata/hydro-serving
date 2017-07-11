package io.hydrosphere.serving.repository.ml.runtime.scikit

/**
  * Created by Bulat on 01.06.2017.
  */
case class ScikitMetadata(
  model: String,
  inputs: List[String],
  outputs: List[String]
)

object ScikitMetadata {
  import io.hydrosphere.serving.repository.util.MapAnyJson._
  import spray.json._

  implicit val scikitMetadataFormat: RootJsonFormat[ScikitMetadata] = jsonFormat3(ScikitMetadata.apply)

  def fromJson(json: String): ScikitMetadata = {
    json.parseJson.convertTo[ScikitMetadata]
  }
}