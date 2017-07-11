package io.hydrosphere.serving.repository.ml

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
  * Created by Bulat on 26.05.2017.
  */
case class Model(
                  name: String,
                  runtime: String,
                  inputs: List[String],
                  outputs: List[String]
                )


object Model extends DefaultJsonProtocol{
  implicit val modelFormat: RootJsonFormat[Model] = jsonFormat4(Model.apply)
}