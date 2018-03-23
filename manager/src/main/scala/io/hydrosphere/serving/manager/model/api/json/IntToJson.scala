package io.hydrosphere.serving.manager.model.api.json

object IntToJson extends TensorJsonLens[IntTensor[_]] {
  override def convert = x => JsNumber.apply(x.asInstanceOf[Int])
}
