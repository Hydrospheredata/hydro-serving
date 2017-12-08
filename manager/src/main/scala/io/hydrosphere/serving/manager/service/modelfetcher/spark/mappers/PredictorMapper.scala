package io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers

import hydroserving.contract.model_field.ModelField
import hydroserving.tensorflow.tensor_info.TensorInfo
import hydroserving.tensorflow.types.DataType.{DT_DOUBLE, DT_STRING}
import io.hydrosphere.serving.manager.service.modelfetcher.spark.SparkModelMetadata
import io.hydrosphere.serving.manager.service.modelfetcher.spark.mappers.SparkMlTypeMapper.{TypeDescription, constructField, scalar}

abstract class PredictorMapper(m: SparkModelMetadata) extends SparkMlTypeMapper(m) {
  def featuresType(sparkModelMetadata: SparkModelMetadata): TypeDescription = SparkMlTypeMapper.featuresVec(sparkModelMetadata)
  def predictionType(sparkModelMetadata: SparkModelMetadata): TypeDescription = scalar(DT_DOUBLE)

  override def labelSchema: Option[List[ModelField]] = {
    val name = m.getParam[String]("labelCol").get
    Some(
      List(
        ModelField(
          name,
          ModelField.InfoOrDict.Info(
            TensorInfo(
              name,
              DT_STRING,
              None
            )
          )
        )
      )
    )
  }

  override def inputSchema: List[ModelField] = {
    List(constructField(m.getParam[String]("featuresCol").get, featuresType(m)))
  }

  override def outputSchema: List[ModelField] = {
    List(constructField(m.getParam[String]("predictionCol").get, predictionType(m)))
  }
}
