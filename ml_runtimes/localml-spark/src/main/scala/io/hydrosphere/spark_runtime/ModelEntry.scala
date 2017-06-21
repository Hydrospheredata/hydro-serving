package io.hydrosphere.spark_runtime

import io.hydrosphere.spark_ml_serving.LocalPipelineModel

/**
  * Created by Bulat on 07.06.2017.
  */
case class ModelEntry(pipeline: LocalPipelineModel, metadata: SparkMetadata)
