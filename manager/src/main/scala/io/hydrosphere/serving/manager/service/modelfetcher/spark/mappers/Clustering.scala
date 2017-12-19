package io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.tensorflow.types.DataType.DT_INT32
import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelMetadata
import io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers.SparkMlTypeMapper.{TypeDescription, scalar}

class KMeansMapper(m: SparkModelMetadata)  extends PredictorMapper(m) {
  override def labelSchema: Option[List[ModelField]] = None
  override def predictionType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_INT32)
}
