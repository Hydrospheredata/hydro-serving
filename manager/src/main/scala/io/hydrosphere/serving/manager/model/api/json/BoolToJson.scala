package io.hydrosphere.serving.manager.model.api.json

object BoolToJson extends TensorToJson[BoolTensor] {
  override def convert = JsBoolean.apply
}
