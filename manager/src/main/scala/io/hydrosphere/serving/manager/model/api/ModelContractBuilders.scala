package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType

object ModelContractBuilders {
  def createUnknownTensorShape(): TensorShapeProto = {
    TensorShapeProto(unknownRank = true)
  }

  def createTensorShape(dims: Seq[Long], unknownRank: Boolean = false): TensorShapeProto = {
    TensorShapeProto(
      dim = dims.map { d =>
        TensorShapeProto.Dim(d)
      },
      unknownRank = unknownRank
    )
  }

  def createTensorInfo(dataType: DataType, shape: Option[Seq[Long]], unknownRank: Boolean = false): TensorInfo = {
    TensorInfo(dataType, shape.map(s => createTensorShape(s, unknownRank)))
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

  def rawTensorModelField(name: String, dataType: DataType, shape: Option[TensorShapeProto]): ModelField = {
    ModelField(name, ModelField.InfoOrSubfields.Info(TensorInfo(dataType, shape)))
  }

  def simpleTensorModelField(name: String, dataType: DataType, shape: Option[Seq[Long]], unknownRank: Boolean = false): ModelField = {
    ModelField(name, ModelField.InfoOrSubfields.Info(createTensorInfo(dataType, shape, unknownRank)))
  }
}