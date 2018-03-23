package io.hydrosphere.serving.manager.model.api.json

object SComplexToJson extends TensorToJson[SComplexTensor] {
  override def convert: Float => JsValue = JsNumber.apply(_)
}
