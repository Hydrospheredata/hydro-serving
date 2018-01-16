package io.hydrosphere.serving.model_api

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType

object ModelContractBuilders {
  import ContractOps.Implicits._

  def createUnknownTensorShape(): TensorShapeProto = {
    TensorShapeProto(unknownRank = true)
  }

  def createTensorShape(dims: Seq[Long]): TensorShapeProto = {
    TensorShapeProto(
      dims.map { d =>
        TensorShapeProto.Dim(d)
      }
    )
  }

  def createTensorInfo(dataType: DataType, shape: Option[Seq[Long]]): TensorInfo = {
    TensorInfo(dataType, shape.map(createTensorShape))
  }

  def complexField(name: String, subFields: Seq[ModelField]): ModelField = {
    ModelField(
      name,
      ModelField.InfoOrSubfields.Subfields(
        ModelField.ComplexField(
          subFields
        )
      )
    )
  }

  def createTensorModelField(name: String, dataType: DataType, shape: Option[Seq[Long]]): ModelField = {
    ModelField(name, ModelField.InfoOrSubfields.Info(createTensorInfo(dataType, shape)))
  }
}
