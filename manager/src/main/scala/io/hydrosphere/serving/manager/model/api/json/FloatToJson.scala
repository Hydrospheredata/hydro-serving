package io.hydrosphere.serving.manager.model.api.json

object FloatToJson extends TensorToJson[FloatTensor] {
  override def convert = JsNumber.apply(_)
}
