package io.hydrosphere.serving.manager.model.api.json

object DoubleToJson extends TensorJsonLens[DoubleTensor] {
  override def convert = JsNumber.apply
}
