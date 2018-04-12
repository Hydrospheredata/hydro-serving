package io.hydrosphere.serving.manager.service.source.fetchers.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.manager.service.source.fetchers.FieldInfo
import io.hydrosphere.serving.manager.service.source.fetchers.spark.SparkModelMetadata
import io.hydrosphere.serving.manager.service.source.fetchers.spark.mappers.SparkMlTypeMapper.constructField

abstract class ProbabilisticClassifierMapper(m: SparkModelMetadata) extends ClassifierMapper(m) {
  def probabilityType(sparkModelMetadata: SparkModelMetadata): FieldInfo = SparkMlTypeMapper.classesVec(sparkModelMetadata)

  override def outputSchema: List[ModelField] = {
    super.outputSchema ++ List(
      constructField(m.getParam("probabilityCol").get, probabilityType(m))
    )
  }
}
