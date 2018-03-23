package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.tensor.Uint64Tensor
import spray.json.JsNumber

object Uint64ToJson extends TensorJsonLens[Uint64Tensor] {
  override def convert = JsNumber.apply
}
