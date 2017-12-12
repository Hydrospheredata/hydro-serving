package io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers

import hydrosphere.contract.model_field.ModelField
import hydrosphere.tensorflow.tensor_info.TensorInfo
import hydrosphere.tensorflow.tensor_shape.TensorShapeProto
import hydrosphere.tensorflow.types.DataType
import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelMetadata

class UntypedMapper(m: SparkModelMetadata)  extends SparkMlTypeMapper(m) {
  private[this] val inputCols = Array("inputCol", "featuresCol")
  private[this] val outputCols = Array("outputCol", "predictionCol", "probabilityCol", "rawPredictionCol")
  private[this] val labelCols = Array("labelCol")

  override def inputSchema: List[ModelField] = {
    inputCols
      .map(m.getParam[String])
      .map {
        _.map {
          inputName =>
            ModelField(
              inputName,
              ModelField.InfoOrDict.Info(
                TensorInfo(
                  inputName,
                  DataType.DT_STRING,
                  Some(
                    TensorShapeProto(unknownRank = true)
                  )
                )
              )
            )
        }
      }
      .filter(_.isDefined)
      .map(_.get)
      .toList
  }

  override def outputSchema: List[ModelField] = {
    outputCols
      .map(m.getParam[String])
      .map {
        _.map {
          inputName =>
            ModelField(
              inputName,
              ModelField.InfoOrDict.Info(
                TensorInfo(
                  inputName,
                  DataType.DT_STRING,
                  Some(
                    TensorShapeProto(unknownRank = true)
                  )
                )
              )
            )
        }
      }
      .filter(_.isDefined)
      .map(_.get)
      .toList
  }
}
