package io.hydrosphere.serving.model_api

trait FieldType
trait ScalarField extends FieldType

case object FAny extends FieldType
case object FAnyScalar extends ScalarField
case object FInteger extends ScalarField
case object FDouble extends ScalarField
case object FString extends ScalarField

case class FMatrix(
  itemType: ScalarField,
  shape: List[Long]
) extends FieldType

object FMatrix {
  def vecVar(scalarField: ScalarField): FMatrix = FMatrix.vecFixed(scalarField, -1)
  def vecFixed(scalarField: ScalarField, size: Long): FMatrix = FMatrix(scalarField, List(size))
}
