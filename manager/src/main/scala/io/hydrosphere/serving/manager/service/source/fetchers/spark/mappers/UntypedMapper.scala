package io.hydrosphere.serving.manager.service.source.fetchers.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.utils.ContractBuilders
import io.hydrosphere.serving.tensorflow.types.DataType
import io.hydrosphere.serving.manager.service.source.fetchers.spark.SparkModelMetadata
import io.hydrosphere.serving.tensorflow.TensorShape.AnyDims

class UntypedMapper(m: SparkModelMetadata) extends SparkMlTypeMapper(m) {
  private[this] val inputCols = Array("inputCol", "featuresCol")
  private[this] val outputCols = Array("outputCol", "predictionCol", "probabilityCol", "rawPredictionCol")
  private[this] val labelCol = "labelCol"

  override def labelSchema: Option[ModelField] = {
    m.getParam[String](labelCol)
      .map { label =>
        ContractBuilders.simpleTensorModelField(
          label,
          DataType.DT_STRING,
          AnyDims()
        )
      }
  }

  override def inputSchema: List[ModelField] = {
    inputCols
      .map(m.getParam[String])
      .flatMap {
        _.map {
          inputName =>
            ContractBuilders.simpleTensorModelField(
              inputName,
              DataType.DT_STRING,
              AnyDims()
            )
        }
      }
      .toList
  }

  override def outputSchema: List[ModelField] = {
    outputCols
      .map(m.getParam[String])
      .flatMap {
        _.map {
          inputName =>
            ContractBuilders.simpleTensorModelField(
              inputName,
              DataType.DT_STRING,
              AnyDims()
            )
        }
      }
      .toList
  }
}
