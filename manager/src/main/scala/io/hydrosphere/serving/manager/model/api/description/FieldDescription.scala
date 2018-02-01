package io.hydrosphere.serving.manager.model.api.description

import io.hydrosphere.serving.tensorflow.types.DataType

case class FieldDescription(
  fieldName: String,
  dataType: String,
  shape: Option[List[Long]]
)