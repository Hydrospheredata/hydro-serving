package io.hydrosphere.serving.model_api

trait FieldType {
  def validate(data: Any): Boolean
  def generate: Any
}

trait ScalarField extends FieldType
case object FAny extends FieldType {
  override def validate(data: Any): Boolean = true // NB assume type is correct and move check to the runtimes

  override def generate: Any = "!!Anything could be here. Consult with developer of the model to get the correct model API!!"
}

case object FAnyScalar extends ScalarField {
  override def validate(data: Any): Boolean = {
    FInteger.validate(data) || FDouble.validate(data) || FString.validate(data)
  }

  override def generate: Any = "!!Scalar!!"
}

case object FInteger extends ScalarField {
  override def validate(data: Any): Boolean = data.isInstanceOf[Int]

  override def generate: Any = 0
}

case object FDouble extends ScalarField {
  override def validate(data: Any): Boolean = data.isInstanceOf[Double]

  override def generate: Any = 0.0
}

case object FString extends ScalarField {
  override def validate(data: Any): Boolean = data.isInstanceOf[String]

  override def generate: Any = "foo"
}

case class FMatrix(
  itemType: ScalarField,
  shape: List[Long]
) extends FieldType {
  override def generate: Seq[Any] = {
    shape.map(_.max(1)).reverse.foldLeft(List.empty[Any]){
      case (Nil, y) =>
        1L.to(y).map(_ => itemType.generate).toList
      case (x, y) =>
        1L.to(y).map(_ => x).toList
    }
  }

  override def validate(data: Any): Boolean = ???
}

object FMatrix {
  def varvec(scalarField: ScalarField) = FMatrix(scalarField, List(-1))
}