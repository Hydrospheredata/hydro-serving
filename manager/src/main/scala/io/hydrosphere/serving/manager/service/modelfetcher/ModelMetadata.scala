package io.hydrosphere.serving.manager.service.modelfetcher

import io.hydrosphere.serving.model.RuntimeType

case class ModelMetadata(
  name: String,
  runtimeType: Option[RuntimeType],
  outputFields: List[ModelField],
  inputFields: List[ModelField]
)

trait FieldType
object FieldType {
  case object FInteger extends FieldType
  case object FFloat extends FieldType
  case class FList(
    itemType: FieldType,
    size: Long
  ) extends FieldType
}

trait ModelField
object ModelField {
  case class UntypedField(
    name: String
  ) extends ModelField
  case class TypedField(
    name: String,
    fieldType: FieldType
  ) extends ModelField

  def extractName(m: ModelField): String = m match {
    case UntypedField(name) => name
    case TypedField(name, _) => name
  }
}