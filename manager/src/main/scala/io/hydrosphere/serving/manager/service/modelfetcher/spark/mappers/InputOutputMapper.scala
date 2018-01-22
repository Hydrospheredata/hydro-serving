package io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelMetadata
import io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers.SparkMlTypeMapper.constructField
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo

abstract class InputOutputMapper(m: SparkModelMetadata) extends SparkMlTypeMapper(m) {
  def inputType(sparkModelMetadata: SparkModelMetadata): TensorInfo
  def outputType(sparkModelMetadata: SparkModelMetadata): TensorInfo

  final def inputSchema: List[ModelField] = {
    List(constructField(m.getParam("inputCol").get, inputType(m)))
  }

  final def outputSchema: List[ModelField]= {
    List(constructField(m.getParam("outputCol").get, outputType(m)))
  }
}
