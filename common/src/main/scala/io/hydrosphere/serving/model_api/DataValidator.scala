//package io.hydrosphere.serving.model_api
//
//class DataValidator(val modelApi: ModelApi) {
//  def validate(data: Seq[Any]): Boolean = modelApi match {
//    case DataFrame(definition) =>
//      val validationResults = definition.map{ fieldDef =>
//        data
//          .map(_.asInstanceOf[Map[String, Any]])
//          .forall{dataRow =>
//            dataRow.get(fieldDef.name) match {
//              case Some(fieldData) =>
//                DataValidator.checkField(fieldData, fieldDef.fieldType)
//              case None =>
//                false
//            }
//          }
//      }
//      validationResults.forall(_== true)
//    case UntypedAPI => true
//  }
//}
//
//object DataValidator {
//  def apply(modelApi: ModelApi): DataValidator = new DataValidator(modelApi)
//
//  def checkField(data: Any, field: FieldType): Boolean = field match {
//    case _: FString.type => data.isInstanceOf[String]
//    case _: FInteger.type => data.isInstanceOf[Int]
//    case _: FDouble.type  => data.isInstanceOf[Double]
//    case _: FAny.type => true
//    case _: FAnyScalar.type => data.isInstanceOf[String] || data.isInstanceOf[Int] || data.isInstanceOf[Double]
//    case FMatrix(item, shape) if data.isInstanceOf[List[Any]] =>
//      checkCollectionField(shape.iterator, data, item).isDefined
//    case _ => false
//  }
//
//  private[this] def checkCollectionField[T](len: Iterator[Long], data: T, item: FieldType): Option[List[Any]] = {
//    def check[T](len: Long, data: List[T]): Option[List[T]] = {
//      val res = data.map {
//        case v: List[T @unchecked] if len == -1 || v.length == len =>
//          Some(v)
//        case _ =>
//          None
//      }
//      if (res.forall(_.isDefined)) {
//        Some(res.flatten.flatten)
//      } else {
//        None
//      }
//    }
//
//    val value = len.next()
//    val res = check(value, data.asInstanceOf[List[Any]])
//    res.flatMap { values =>
//      if (!len.hasNext) {
//        val isHomogeneousList = values.forall(x => values.head.getClass == x.getClass)
//        val isCorrectType = DataValidator.checkField(values, item)
//        if(isHomogeneousList && isCorrectType) {
//          Some(values)
//        } else {
//          None
//        }
//      } else {
//        checkCollectionField(len, values, item)
//      }
//    }
//  }
//
//}