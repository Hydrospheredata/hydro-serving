package io.hydrosphere.serving.manager.model.api.json

object StringToJson extends TensorToJson[StringTensor] {
  override def convert = JsString.apply
}
