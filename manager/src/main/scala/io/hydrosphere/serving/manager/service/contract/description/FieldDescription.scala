package io.hydrosphere.serving.manager.service.contract.description

import io.hydrosphere.serving.manager.util.CommonJsonSupport._
import io.hydrosphere.serving.tensorflow.types.DataType

case class FieldDescription(
  fieldName: String,
  dataType: DataType,
  shape: Option[List[Long]]
)
object FieldDescription {
  implicit val fieldDescFormat = jsonFormat3(FieldDescription.apply)
}