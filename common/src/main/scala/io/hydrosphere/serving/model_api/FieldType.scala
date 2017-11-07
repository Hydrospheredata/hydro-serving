package io.hydrosphere.serving.model_api

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

trait FieldVisitor {
  def visit(field: FieldType): Any
}

class ApiGenerator(val modelApi: ModelApi) extends FieldVisitor {
  def generate: Any = modelApi match {
    case DataFrame(definition) =>
      definition.map(x => x.name -> visit(x.fieldType)).toMap
    case UntypedAPI =>
      visit(FAny)
  }

  override def visit(field: FieldType): Any = field match {
    case _: FString.type => "foo"
    case _: FInteger.type => 0
    case _: FDouble.type  => 0.5
    case _: FAny.type => "any"
    case _: FAnyScalar.type => "any_scalar"
    case FMatrix(item, shape) =>
      shape.map(_.max(1)).reverse.foldLeft(List.empty[Any]){
        case (Nil, y) =>
          1L.to(y).map(_ => visit(item)).toList
        case (x, y) =>
          1L.to(y).map(_ => x).toList
      }
  }
}

class ApiValidator(data: Any) extends FieldVisitor {

  private[this] def check[T](len: Long, data: List[T]): Option[List[T]] = {
    val res = data.map {
      case v: List[T @unchecked] if len == -1 || v.length == len =>
        Some(v)
      case _ =>
        None
    }
    if (res.forall(_.isDefined)) {
      Some(res.flatten.flatten)
    } else {
      None
    }
  }

  private[this] def check[T](len: Iterator[Long], data: T, item: FieldType): Option[List[Any]] = {
    val value = len.next()
    val res = check(value, data.asInstanceOf[List[Any]])
    res.flatMap { values =>
      if (!len.hasNext) {
        val isHomogenousList = values.forall(x => values.head.getClass == x.getClass)
        val isCorrectType = new ApiValidator(values).visit(item)
        if(isHomogenousList && isCorrectType) {
          Some(values)
        } else {
          None
        }
      } else {
        check(len, values, item)
      }
    }
  }

  override def visit(field: FieldType): Boolean = field match {
    case _: FString.type => data.isInstanceOf[String]
    case _: FInteger.type => data.isInstanceOf[Int]
    case _: FDouble.type  => data.isInstanceOf[Double]
    case _: FAny.type => true
    case _: FAnyScalar.type => data.isInstanceOf[String] || data.isInstanceOf[Int] || data.isInstanceOf[Double]
    case FMatrix(item, shape) if data.isInstanceOf[List[Any]] =>
      check(shape.iterator, data, item).isDefined
    case _ => false
  }
}

object ApiValidator {
  def validate(modelApi: ModelApi, data: Seq[Any]): Boolean = modelApi match {
    case DataFrame(definition) =>
      val validationResults =definition.map{ fieldDef =>
        data
          .map(_.asInstanceOf[Map[String, Any]])
          .forall{dataRow =>
            dataRow.get(fieldDef.name) match {
              case Some(fieldData) =>
                new ApiValidator(fieldData).visit(fieldDef.fieldType)
              case None =>
                false
            }
          }
      }
      validationResults.forall(_== true)
    case UntypedAPI => true
  }
}