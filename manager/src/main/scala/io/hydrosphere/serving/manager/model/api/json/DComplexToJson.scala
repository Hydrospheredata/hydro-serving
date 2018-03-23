package io.hydrosphere.serving.manager.model.api.json

object DComplexToJson extends TensorJsonLens[DComplexTensor] {
  override def convert: Double => JsValue = JsNumber.apply
}
