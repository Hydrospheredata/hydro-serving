package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.tensor.MapTensor
import spray.json.JsObject

object MapToJson extends TensorJsonLens[MapTensor] {
  override def convert = { dict =>
    val fields = dict.mapValues(TensorJsonLens.toJson)
    JsObject(fields)
  }
}
