package io.hydrosphere.serving.manager.model.api.json

object DoubleToJson extends TensorToJson[DoubleTensor] {
  override def convert = JsNumber.apply
}
