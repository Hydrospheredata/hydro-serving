package io.hydrosphere.serving.model_api

case class ModelField(
  name: String,
  fieldType: FieldType
) {
  def validate(rowName: String, rowData: Any): Boolean = {
    (name == rowName) && fieldType.validate(rowData)
  }

  def generate: (String, Any) = name -> fieldType.generate
}

object ModelField {
  def untyped(name: String) = ModelField(name, FAny)
}