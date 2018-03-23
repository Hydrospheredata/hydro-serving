package io.hydrosphere.serving.manager.model.api.json

object SComplexToJson extends TensorJsonLens[SComplexTensor] {
  override def convert: Float => JsValue = JsNumber.apply(_)
}
