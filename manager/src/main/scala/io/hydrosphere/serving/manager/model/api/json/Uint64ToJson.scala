package io.hydrosphere.serving.manager.model.api.json

object Uint64ToJson extends TensorJsonLens[Uint64Tensor] {
  override def convert = JsNumber.apply
}
