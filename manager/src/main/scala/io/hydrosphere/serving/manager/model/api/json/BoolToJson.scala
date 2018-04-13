package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.tensor.BoolTensor
import spray.json.{JsBoolean, JsValue}

object BoolToJson extends TensorJsonLens[BoolTensor] {
  override def convert = JsBoolean.apply
}
