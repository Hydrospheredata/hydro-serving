package io.hydrosphere.serving.model_api

import io.hydrosphere.serving.model.RuntimeType

case class ModelMetadata(
  name: String,
  runtimeType: Option[RuntimeType],
  outputFields: ModelApi,
  inputFields: ModelApi
) {
  def validateInput(data: Seq[Any]): Boolean = this.inputFields.validate(data)

  def mockInput: Seq[Any] = this.inputFields.generate
}