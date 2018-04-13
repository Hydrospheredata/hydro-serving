package io.hydrosphere.serving.manager.model.api.json

import io.hydrosphere.serving.tensorflow.tensor.SComplexTensor
import spray.json.{JsNumber, JsValue}

object SComplexToJson extends TensorJsonLens[SComplexTensor] {
  override def convert: Float => JsValue = JsNumber.apply(_)
}
