package io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.tensorflow.types.DataType.DT_INT32
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark.SparkModelMetadata
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.spark.mappers.SparkMlTypeMapper.scalar

class KMeansMapper(m: SparkModelMetadata)  extends PredictorMapper(m) {
  override def labelSchema: Option[ModelField] = None
  override def predictionType(sparkModelMetadata: SparkModelMetadata) = scalar(DT_INT32)
}
