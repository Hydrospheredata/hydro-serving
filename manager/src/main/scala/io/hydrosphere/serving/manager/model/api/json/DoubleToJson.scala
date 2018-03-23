package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.tensor.DoubleTensor
import spray.json.JsNumber

object DoubleToJson extends TensorJsonLens[DoubleTensor] {
  override def convert = JsNumber.apply
}
