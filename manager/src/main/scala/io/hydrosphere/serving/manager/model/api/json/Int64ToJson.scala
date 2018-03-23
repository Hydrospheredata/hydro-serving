package io.hydrosphere.serving.manager.model.api.json

object Int64ToJson extends TensorJsonLens[Int64Tensor] {
  override def convert = JsNumber.apply
}
