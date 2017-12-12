package io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo
import io.hydrosphere.serving.tensorflow.tensor_shape.TensorShapeProto
import io.hydrosphere.serving.tensorflow.types.DataType
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
