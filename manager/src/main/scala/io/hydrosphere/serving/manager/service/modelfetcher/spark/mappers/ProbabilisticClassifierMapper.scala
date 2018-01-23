package io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelMetadata
import io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers.SparkMlTypeMapper.constructField
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo

abstract class ProbabilisticClassifierMapper(m: SparkModelMetadata) extends ClassifierMapper(m) {
  def probabilityType(sparkModelMetadata: SparkModelMetadata): TensorInfo = SparkMlTypeMapper.classesVec(sparkModelMetadata)

  override def outputSchema: List[ModelField] = {
    super.outputSchema ++ List(
      constructField(m.getParam("probabilityCol").get, probabilityType(m))
    )
  }
}
