package io.hydrosphere.serving.manager.model.api.tensor_builder

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.utils.validation._
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.tensor._
import io.hydrosphere.serving.tensorflow.types.DataType
import spray.json._


class InfoFieldBuilder(val field: ModelField, val dataType: DataType) {

  def convert(data: JsValue): Either[ValidationError, TypedTensor[_]] = {
    data match {
      // collection
      case JsArray(elements) => process(elements)
      // scalar
      case str: JsString => process(Seq(str))
      case num: JsNumber => process(Seq(num))
      case bool: JsBoolean => process(Seq(bool))
      // invalid
      case _ => Left(new IncompatibleFieldTypeError(field.name, dataType))
    }
  }

  def process(data: Seq[JsValue]): Either[ValidationError, TypedTensor[_]] = {
    val reshapedData = field.shape match {
      case Some(_) => flatten(data)
      case None => data
    }
    val factory = TypedTensorFactory(dataType)
    val convertedData = factory match {
      case FloatTensor | SComplexTensor => reshapedData.map(_.asInstanceOf[JsNumber].value.floatValue())
      case DoubleTensor | DComplexTensor => reshapedData.map(_.asInstanceOf[JsNumber].value.doubleValue())
      case Uint64Tensor | Int64Tensor => reshapedData.map(_.asInstanceOf[JsNumber].value.longValue())
      case Int8Tensor | Uint8Tensor |
           Int16Tensor | Uint16Tensor |
           Int32Tensor | Uint32Tensor => reshapedData.map(_.asInstanceOf[JsNumber].value.intValue())
      case StringTensor => reshapedData.map(_.asInstanceOf[JsString].value)
      case BoolTensor => reshapedData.map(_.asInstanceOf[JsBoolean].value)
    }
    toTensor(factory, convertedData)
  }

  def toTensor[T <: TypedTensor[_]](factory: TypedTensorFactory[T], flatData: Seq[Any]): Either[ValidationError, T] = {
    factory.createFromAny(flatData, TensorShape.fromProto(field.shape)) match {
      case Some(tensor) => Right(tensor)
      case None => Left(new ValidationError(s"Can't create a tensor with $dataType type and ${field.shape} shape for [$flatData]") {})
    }
  }

  private def flatten(arr: Seq[JsValue]): Seq[JsValue] = {
    arr.flatMap {
      case arr: JsArray => flatten(arr.elements)
      case value => List(value)
    }
  }

}