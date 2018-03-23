package io.hydrosphere.serving.manager.model.api.json

object DComplexToJson extends TensorToJson[DComplexTensor] {
  override def convert: Double => JsValue = JsNumber.apply
}
