package io.hydrosphere.serving.model_api

case class ModelField(
  name: String,
  fieldType: FieldType
)

object ModelField {
  def untyped(name: String) = ModelField(name, FAny)
}