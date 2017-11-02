gipackage io.hydrosphere.serving.model_api

trait FieldType {
  def accept[R](visitor: FieldType => R) = visitor(this)
}

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
  def varvec(scalarField: ScalarField): FMatrix = FMatrix.vec(scalarField, -1)
  def vec(scalarField: ScalarField, size: Long): FMatrix = FMatrix(scalarField, List(size))
}
