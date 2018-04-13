package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.tensor.Int64Tensor
import spray.json.JsNumber

object Int64ToJson extends TensorJsonLens[Int64Tensor] {
  override def convert = JsNumber.apply
}
