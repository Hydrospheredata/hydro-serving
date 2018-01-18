package io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.tensorflow.types.DataType
import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelMetadata
import io.hydrosphere.serving.manager.model.api.ModelContractBuilders

class UntypedMapper(m: SparkModelMetadata) extends SparkMlTypeMapper(m) {
  private[this] val inputCols = Array("inputCol", "featuresCol")
  private[this] val outputCols = Array("outputCol", "predictionCol", "probabilityCol", "rawPredictionCol")
  //private[this] val labelCols = Array("labelCol")

  override def inputSchema: List[ModelField] = {
    inputCols
      .map(m.getParam[String])
      .flatMap {
        _.map {
          inputName =>
            ModelContractBuilders.simpleTensorModelField(
              inputName,
              DataType.DT_STRING,
              Some(Seq.empty),
              unknownRank = true
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
            ModelContractBuilders.simpleTensorModelField(
              inputName,
              DataType.DT_STRING,
              Some(Seq.empty),
              unknownRank = true
            )
        }
      }
      .toList
  }
}
