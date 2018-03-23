package io.hydrosphere.serving.manager.model.api.json

object MapToJson extends TensorToJson[MapTensor] {
  override def convert = { dict =>
    val fields = dict.mapValues(TensorToJson.toJson)
    JsObject(fields)
  }
}
