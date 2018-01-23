package io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.tensorflow.types.DataType.{DT_DOUBLE, DT_STRING}
import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelMetadata
import io.hydrosphere.serving.manager.model.api.ContractBuilders
import io.hydrosphere.serving.tensorflow.tensor_info.TensorInfo

abstract class PredictorMapper(m: SparkModelMetadata) extends SparkMlTypeMapper(m) {
  def featuresType(sparkModelMetadata: SparkModelMetadata): TensorInfo = SparkMlTypeMapper.featuresVec(sparkModelMetadata)
  def predictionType(sparkModelMetadata: SparkModelMetadata): TensorInfo = SparkMlTypeMapper.scalar(DT_DOUBLE)

  override def labelSchema: Option[ModelField] = {
    val name = m.getParam[String]("labelCol").get
    Some(
      ContractBuilders.rawTensorModelField(name, DT_STRING, None)
    )
  }

  override def inputSchema: List[ModelField] = {
    List(SparkMlTypeMapper.constructField(m.getParam[String]("featuresCol").get, featuresType(m)))
  }

  override def outputSchema: List[ModelField] = {
    List(SparkMlTypeMapper.constructField(m.getParam[String]("predictionCol").get, predictionType(m)))
  }
}
