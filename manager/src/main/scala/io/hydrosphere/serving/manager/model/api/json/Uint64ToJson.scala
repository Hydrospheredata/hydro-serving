package io.hydrosphere.serving.manager.model.api.json

object Uint64ToJson extends TensorToJson[Uint64Tensor] {
  override def convert = JsNumber.apply
}
