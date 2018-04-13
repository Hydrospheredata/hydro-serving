package io.hydrosphere.serving.manager.service.source.fetchers.spark.mappers

import io.hydrosphere.serving.contract.model_field.ModelField
import io.hydrosphere.serving.contract.utils.ContractBuilders
import io.hydrosphere.serving.manager.service.source.fetchers.FieldInfo
import io.hydrosphere.serving.tensorflow.types.DataType.{DT_DOUBLE, DT_STRING}
import io.hydrosphere.serving.manager.service.source.fetchers.spark.SparkModelMetadata

abstract class PredictorMapper(m: SparkModelMetadata) extends SparkMlTypeMapper(m) {
  def featuresType(sparkModelMetadata: SparkModelMetadata): FieldInfo = SparkMlTypeMapper.featuresVec(sparkModelMetadata)
  def predictionType(sparkModelMetadata: SparkModelMetadata): FieldInfo = SparkMlTypeMapper.scalar(DT_DOUBLE)

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
