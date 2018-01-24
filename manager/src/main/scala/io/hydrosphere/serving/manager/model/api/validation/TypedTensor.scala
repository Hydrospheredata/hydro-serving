package io.hydrosphere.serving.manager.model.api.validation

import com.google.protobuf.ByteString
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.types.DataType
import io.hydrosphere.serving.tensorflow.types.DataType.{DT_BOOL, DT_COMPLEX128, DT_COMPLEX64, DT_DOUBLE, DT_FLOAT, DT_INT16, DT_INT32, DT_INT64, DT_INT8, DT_QINT16, DT_QINT32, DT_QINT8, DT_QUINT16, DT_QUINT8, DT_STRING, DT_UINT16, DT_UINT32, DT_UINT64, DT_UINT8}

import scala.reflect.ClassTag

/**
  * At one time, `TensorProto` can only have one type of data.
  * `TypedTensor` is a wrapper that ensures it, and provides simple access.
  * @tparam T type of data field
  */
sealed trait TypedTensor[T] {
  /**
    * Returns tensor contents from a field as flat `Seq`
    * @param tensorProto target tensor
    * @return flat data `Seq`
    */
  def get(tensorProto: TensorProto): Seq[T]

  /**
    * Puts data to a field
    * @param tensorProto target tensor
    * @param data data
    * @return tensor with new data
    */
  def put(tensorProto: TensorProto, data: Seq[T]): TensorProto

  /**
    * Tries to convert data to tensor-specific type and puts it to a field
    * @param tensorProto target tensor
    * @param data data
    * @param ct class tag to retrieve class info
    * @return tensor with new data or error
    */
  def putAny(tensorProto: TensorProto, data: Seq[Any])(implicit ct: ClassTag[T]): Either[ValidationError, TensorProto] = {
    castData(data).right.map { converted =>
      put(tensorProto, converted)
    }
  }

  /**
    * Tries to convert `data` to tensor-specific type.
    * @param data target data
    * @param ct class tag to retrieve class info
    * @return converted data or error
    */
  def castData(data: Seq[Any])(implicit ct: ClassTag[T]): Either[ValidationError, Seq[T]] = {
    try {
      Right(data.map(_.asInstanceOf[T]))
    } catch {
      case _: ClassCastException => Left(new InvalidFieldData(ct.runtimeClass))
    }
  }
}

object TypedTensor{
  def apply(dataType: DataType): TypedTensor[_] = {
    dataType match {
      case DT_FLOAT => FloatTensor

      case DT_DOUBLE => DoubleTensor

      case DT_INT8 | DT_INT16 | DT_INT32 |
           DT_QINT8 | DT_QINT16 | DT_QINT32 => IntTensor

      case DT_UINT8 | DT_UINT16 | DT_UINT32 |
           DT_QUINT8 | DT_QUINT16 => UintTensor

      case DT_INT64 => Int64Tensor

      case DT_UINT64 => Uint64Tensor

      case DT_COMPLEX64 => SComplexTensor
      case DT_COMPLEX128 => DComplexTensor

      case DT_STRING => StringTensor
      case DT_BOOL => BoolTensor

      case x => throw new UnsupportedFieldTypeError(x)
    }
  }
  /**
    * Creates tensor with `data` and `tensorInfo`
    * @param data contents to be put in tensor
    * @param tensorInfo tensor info
    * @return tensor with data or error
    */
  def constructTensor(data: Seq[Any], tensorInfo: TensorInfo): Either[ValidationError, TensorProto] = {
    val typedTensor = TypedTensor(tensorInfo.dtype)

    val tensor = TensorProto(dtype = tensorInfo.dtype, tensorShape = tensorInfo.tensorShape)
    typedTensor.putAny(tensor, data)
  }


  object FloatTensor extends TypedTensor[Float] {
    override def get(tensorProto: TensorProto): Seq[Float] = tensorProto.floatVal
    override def put(tensorProto: TensorProto, data: Seq[Float]): TensorProto = tensorProto.addAllFloatVal(data)
  }

  object SComplexTensor extends TypedTensor[Float] {
    override def get(tensorProto: TensorProto): Seq[Float] = tensorProto.scomplexVal
    override def put(tensorProto: TensorProto, data: Seq[Float]): TensorProto = tensorProto.addAllScomplexVal(data)
  }

  object DoubleTensor extends TypedTensor[Double] {
    override def get(tensorProto: TensorProto): Seq[Double] = tensorProto.doubleVal
    override def put(tensorProto: TensorProto, data: Seq[Double]): TensorProto = tensorProto.addAllDoubleVal(data)
  }

  object DComplexTensor extends TypedTensor[Double] {
    override def get(tensorProto: TensorProto): Seq[Double] = tensorProto.dcomplexVal
    override def put(tensorProto: TensorProto, data: Seq[Double]): TensorProto = tensorProto.addAllDcomplexVal(data)
  }

  object Uint64Tensor extends TypedTensor[Long] {
    override def get(tensorProto: TensorProto): Seq[Long] = tensorProto.uint64Val
    override def put(tensorProto: TensorProto, data: Seq[Long]): TensorProto = tensorProto.addAllUint64Val(data)
  }

  object Int64Tensor extends TypedTensor[Long] {
    override def get(tensorProto: TensorProto): Seq[Long] = tensorProto.int64Val
    override def put(tensorProto: TensorProto, data: Seq[Long]): TensorProto = tensorProto.addAllInt64Val(data)
  }

  object IntTensor extends TypedTensor[Int] {
    override def get(tensorProto: TensorProto): Seq[Int] = tensorProto.intVal
    override def put(tensorProto: TensorProto, data: Seq[Int]): TensorProto = tensorProto.addAllIntVal(data)
  }

  object UintTensor extends TypedTensor[Int] {
    override def get(tensorProto: TensorProto): Seq[Int] = tensorProto.uint32Val
    override def put(tensorProto: TensorProto, data: Seq[Int]): TensorProto = tensorProto.addAllUint32Val(data)
  }

  object StringTensor extends TypedTensor[String] {
    override def get(tensorProto: TensorProto): Seq[String] = tensorProto.stringVal.map(_.toStringUtf8)
    override def put(tensorProto: TensorProto, data: Seq[String]): TensorProto = tensorProto.addAllStringVal(data.map(ByteString.copyFromUtf8))
  }

  object BoolTensor extends TypedTensor[Boolean] {
    override def get(tensorProto: TensorProto): Seq[Boolean] = tensorProto.boolVal
    override def put(tensorProto: TensorProto, data: Seq[Boolean]): TensorProto = tensorProto.addAllBoolVal(data)
  }
}