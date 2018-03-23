package io.hydrosphere.serving.manager.model.api.json

object MapToJson extends TensorJsonLens[MapTensor] {
  override def convert = { dict =>
    val fields = dict.mapValues(TensorJsonLens.toJson)
    JsObject(fields)
  }
}
