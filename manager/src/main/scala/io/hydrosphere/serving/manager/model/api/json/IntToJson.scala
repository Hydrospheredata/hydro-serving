package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.tensor.IntTensor
import spray.json.JsNumber

object IntToJson extends TensorJsonLens[IntTensor[_]] {
  override def convert = x => JsNumber.apply(x.asInstanceOf[Int])
}
