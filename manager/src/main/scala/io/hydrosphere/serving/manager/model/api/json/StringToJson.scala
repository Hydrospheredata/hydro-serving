package io.hydrosphere.serving.manager.model.api.json

object StringToJson extends TensorJsonLens[StringTensor] {
  override def convert = JsString.apply
}
