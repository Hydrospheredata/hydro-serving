package io.hydrosphere.serving.manager.model.api.json

object FloatToJson extends TensorJsonLens[FloatTensor] {
  override def convert = JsNumber.apply(_)
}
