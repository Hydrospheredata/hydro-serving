package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.tensorflow.tensor.TensorProto
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
import DataType._
import com.google.protobuf.ByteString
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField.InfoOrSubfields.{Empty, Info, Subfields}
import io.hydrosphere.serving.contract.model_signature.ModelSignature


class DataGenerator(val modelApi: ModelSignature) {
  def generateInputs: Map[String, TensorProto] = {
    modelApi.inputs.flatMap(DataGenerator.generateField).toMap
  }

  def generateOutputs: Map[String, TensorProto] = {
    modelApi.outputs.flatMap(DataGenerator.generateField).toMap
  }
}

object DataGenerator {
  def apply(modelApi: ModelSignature): DataGenerator = new DataGenerator(modelApi)

  def forContract(modelContract: ModelContract, signature: String): Option[DataGenerator] = {
    modelContract.signatures.find(_.signatureName == signature).map(DataGenerator.apply)
  }

  def generateData(dataType: DataType): Any = {
    dataType match {
      case DT_FLOAT | DT_COMPLEX64 => 1.0F
      case DT_DOUBLE | DT_COMPLEX128 => 1.0D
      case DT_INT8 | DT_INT16 | DT_INT32 |
           DT_UINT8 | DT_UINT16 | DT_UINT32 |
           DT_QINT8 | DT_QINT16 | DT_QINT32 |
           DT_QUINT8 | DT_QUINT16 => 1
      case DT_INT64 | DT_UINT64 => 1L
      case DT_STRING => "foo"
      case DT_BOOL => true

      case DT_INVALID => throw new IllegalArgumentException(s"Can't convert data to DT_INVALID  has an invalid dtype")
      case DT_VARIANT => throw new IllegalArgumentException(s"Cannot process DT_VARIANT Tensor. Not supported yet.")
      case Unrecognized(value) => throw new IllegalArgumentException(s"Cannot process Tensor with Unrecognized($value) dtype")
      case x => throw new IllegalArgumentException(s"Cannot process Tensor with $x dtype")// refs
    }
  }

  def generateData(dataType: DataType, shape: Option[TensorShapeProto]): List[Any] = {
    shape match {
      case Some(sh) => sh.dim.map(_.size.max(1)).reverse.foldLeft(List.empty[Any]){
        case (Nil, y) =>
          1L.to(y).map(_ => generateData(dataType)).toList
        case (x, y) =>
          1L.to(y).map(_ => x).toList
      }
      case None => List(generateData(dataType))
    }
  }

  def generateTensor(tensorInfo: TensorInfo): TensorProto = {
    val tensor = TensorProto(dtype = tensorInfo.dtype, tensorShape = tensorInfo.tensorShape)
    val data = generateData(tensorInfo.dtype, tensorInfo.tensorShape)
    tensorInfo.dtype match {
      case DT_INVALID => throw new IllegalArgumentException(s"can't convert data to DT_INVALID  has an invalid dtype")
      case DT_FLOAT => tensor.copy(floatVal = data.map(_.asInstanceOf[Float]))
      case DT_DOUBLE => tensor.copy(doubleVal = data.map(_.asInstanceOf[Double]))
      case DT_INT8 | DT_INT16 | DT_INT32 => tensor.copy(intVal = data.map(_.asInstanceOf[Int]))
      case DT_UINT8 | DT_UINT16 | DT_UINT32 => tensor.copy(uint32Val = data.map(_.asInstanceOf[Int]))
      case DT_INT64 => tensor.copy(int64Val = data.map(_.asInstanceOf[Long]))
      case DT_UINT64 => tensor.copy(uint64Val = data.map(_.asInstanceOf[Long]))

      case DT_QINT8 | DT_QINT16 | DT_QINT32 => tensor.copy(intVal = data.map(_.asInstanceOf[Int]))
      case DT_QUINT8 | DT_QUINT16 => tensor.copy(uint32Val = data.map(_.asInstanceOf[Int]))
      case DT_COMPLEX64 => tensor.copy(scomplexVal = data.map(_.asInstanceOf[Float]))
      case DT_COMPLEX128 => tensor.copy(dcomplexVal = data.map(_.asInstanceOf[Double]))

      case DT_STRING => tensor.copy(stringVal = data.map(x => ByteString.copyFromUtf8(x.toString)))
      case DT_BOOL => tensor.copy(boolVal = data.map(_.asInstanceOf[Boolean]))
      case DT_VARIANT => throw new IllegalArgumentException(s"Cannot process DT_VARIANT Tensor. Not supported yet.")

      case Unrecognized(value) => throw new IllegalArgumentException(s"Cannot process Tensor with Unrecognized($value) dtype")
      case x => throw new IllegalArgumentException(s"Cannot process Tensor with $x dtype")// refs
    }
  }

  def generateField(field: ModelField): Map[String, TensorProto] = {
    val tensor = field.infoOrSubfields match {
      case Empty => TensorProto()
      case Info(value) => generateTensor(value)
      case Subfields(value) =>
        val tensor = TensorProto(dtype = DataType.DT_MAP)
        tensor.withMapVal(
          value.data.flatMap(generateField).toMap
        )
    }
    Map(field.fieldName -> tensor)
  }
}