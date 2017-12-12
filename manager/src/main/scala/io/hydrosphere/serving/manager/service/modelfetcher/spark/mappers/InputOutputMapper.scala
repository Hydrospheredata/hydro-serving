package io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelMetadata
import io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers.SparkMlTypeMapper.{TypeDescription, constructField}

abstract class InputOutputMapper(m: SparkModelMetadata) extends SparkMlTypeMapper(m) {
  def inputType(sparkModelMetadata: SparkModelMetadata): TypeDescription
  def outputType(sparkModelMetadata: SparkModelMetadata): TypeDescription

  final def inputSchema: List[ModelField] = {
    List(constructField(m.getParam("inputCol").get, inputType(m)))
  }

  final def outputSchema: List[ModelField]= {
    List(constructField(m.getParam("outputCol").get, outputType(m)))
  }
}
