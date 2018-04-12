package io.hydrosphere.serving.manager.service.source.fetchers.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.manager.service.source.fetchers.FieldInfo
import io.hydrosphere.serving.manager.service.source.fetchers.spark.SparkModelMetadata
import io.hydrosphere.serving.manager.service.source.fetchers.spark.mappers.SparkMlTypeMapper.constructField

abstract class InputOutputMapper(m: SparkModelMetadata) extends SparkMlTypeMapper(m) {
  def inputType(sparkModelMetadata: SparkModelMetadata): FieldInfo
  def outputType(sparkModelMetadata: SparkModelMetadata): FieldInfo

  final def inputSchema: List[ModelField] = {
    List(constructField(m.getParam("inputCol").get, inputType(m)))
  }

  final def outputSchema: List[ModelField]= {
    List(constructField(m.getParam("outputCol").get, outputType(m)))
  }
}
