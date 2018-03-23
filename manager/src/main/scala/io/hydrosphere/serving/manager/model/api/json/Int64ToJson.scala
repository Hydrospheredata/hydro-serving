package io.hydrosphere.serving.manager.model.api.json

object Int64ToJson extends TensorToJson[Int64Tensor] {
  override def convert = JsNumber.apply
}
