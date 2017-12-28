package io.hydrosphere.serving.manager.model.api

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType

object ModelContractBuilders {
  import io.hydrosphere.serving.manager.model.api.ContractOps.Implicits._

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

  def createDictModelField(name: String, map: Map[String, TensorInfo]): ModelField = {
    ModelField(
      name,
      ModelField.InfoOrDict.Dict(
        ModelField.Dict(
          map.map{
            case (key, value) =>  key -> createTensorModelField(value.name, value.dtype, value.tensorShape.map(_.toDimList))
          }
        )
      )
    )
  }

  def createTensorModelField(name: String, dataType: DataType, shape: Option[Seq[Long]]): ModelField = {
    ModelField(name, ModelField.InfoOrDict.Info(createTensorInfo(name, dataType, shape)))
  }
}