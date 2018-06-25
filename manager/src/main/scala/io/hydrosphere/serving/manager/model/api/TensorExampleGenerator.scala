package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.model_field.ModelField.TypeOrSubfields.{Dtype, Empty, Subfields}
import io.hydrosphere.serving.contract.model_signature.ModelSignature
import io.hydrosphere.serving.tensorflow.TensorShape
import io.hydrosphere.serving.tensorflow.TensorShape.{AnyDims, Dims}
import io.hydrosphere.serving.tensorflow.tensor.{MapTensor, TypedTensor, TypedTensorFactory}
import io.hydrosphere.serving.tensorflow.types.DataType
import io.hydrosphere.serving.tensorflow.types.DataType.{DT_BOOL, DT_COMPLEX128, DT_COMPLEX64, DT_DOUBLE, DT_FLOAT, DT_INT16, DT_INT32, DT_INT64, DT_INT8, DT_INVALID, DT_QINT16, DT_QINT32, DT_QINT8, DT_QUINT16, DT_QUINT8, DT_STRING, DT_UINT16, DT_UINT32, DT_UINT64, DT_UINT8}

case class TensorExampleGenerator(modelApi: ModelSignature) {
  def inputs: Map[String, TypedTensor[_]] = {
    modelApi.inputs.flatMap(TensorExampleGenerator.generateField).toMap
  }

  def outputs: Map[String, TypedTensor[_]] = {
    modelApi.outputs.flatMap(TensorExampleGenerator.generateField).toMap
  }
}

object TensorExampleGenerator {
  def forContract(modelContract: ModelContract, signature: String): Option[TensorExampleGenerator] = {
    modelContract.signatures.find(_.signatureName == signature).map(TensorExampleGenerator.apply)
  }

  def generateScalarData[T <: DataType](dataType: T): Any = {
    dataType match {
      case DT_FLOAT | DT_COMPLEX64 => 1.0F
      case DT_DOUBLE | DT_COMPLEX128 => 1.0D
      case DT_INT8 | DT_INT16 | DT_INT32 | DT_UINT8 | DT_UINT16 | DT_UINT32 | DT_QINT8 | DT_QINT16 |
           DT_QINT32 | DT_QUINT8 | DT_QUINT16 =>
        1
      case DT_INT64 | DT_UINT64 => 1L
      case DT_STRING => "foo"
      case DT_BOOL => true

      case DT_INVALID =>
        throw new IllegalArgumentException(
          s"Can't convert data to DT_INVALID"
        )
      case x => throw new IllegalArgumentException(s"Cannot process Tensor with $x dtype") // refs
    }
  }

  def createFlatTensor[T](shape: TensorShape, generator: => T): Seq[T] = {
    shape match {
      case AnyDims() => List(generator)
      case Dims(dims, _) if dims.isEmpty => List(generator)
      case Dims(dims, _) =>
        val flatLen = dims.map(_.max(1)).product
        (1L to flatLen).map(_ => generator) // mat
    }
  }

  def generateTensor(shape: TensorShape, dtype: DataType): Option[TypedTensor[_]] = {
    val factory = TypedTensorFactory(dtype)
    val data = createFlatTensor(shape, generateScalarData(dtype))
    factory.createFromAny(data, shape)
  }

  def generateField(field: ModelField): Map[String, TypedTensor[_]] = {
    val shape = TensorShape(field.shape)
    val fieldValue = field.typeOrSubfields match {
      case Empty => None
      case Dtype(value) => generateTensor(shape, value)
      case Subfields(value) => generateNestedTensor(shape, value)
    }
    fieldValue.map(x => field.name -> x).toMap
  }

  private def generateNestedTensor(shape: TensorShape, value: ModelField.Subfield): Option[MapTensor] = {
    val map = generateMap(value)
    val tensorData = createFlatTensor(shape, map)
    Some(MapTensor(shape, tensorData))
  }

  private def generateMap(value: ModelField.Subfield): Map[String, TypedTensor[_]] = {
    value.data.flatMap(generateField).toMap
  }
}