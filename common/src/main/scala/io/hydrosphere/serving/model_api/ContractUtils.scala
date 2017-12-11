package io.hydrosphere.serving.model_api

import hydroserving.contract.model_field.ModelField
import hydroserving.tensorflow.tensor_info.TensorInfo
import hydroserving.tensorflow.tensor_shape.TensorShapeProto
import hydroserving.tensorflow.types.DataType

object ContractUtils {
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

  def createTensorInfo(name: String, dataType: DataType, shape: Option[Seq[Long]]): TensorInfo = {
    TensorInfo(name, dataType, shape.map(createTensorShape))
  }

  def createTensorModelField(name: String, dataType: DataType, shape: Option[Seq[Long]]): ModelField = {
    ModelField(name, ModelField.InfoOrDict.Info(createTensorInfo(name, dataType, shape)))
  }
}
