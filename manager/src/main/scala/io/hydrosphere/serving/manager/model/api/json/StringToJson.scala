package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.tensor.StringTensor
import spray.json.JsString

object StringToJson extends TensorJsonLens[StringTensor] {
  override def convert = JsString.apply
}
