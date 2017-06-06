package org.kineticcookie.serve

import org.apache.spark.ml.{PipelineModel, Transformer}

/**
  * Created by Bulat on 23.05.2017.
  */
object SparkUtils {
  private implicit class PumpedTransformer(transformer: Transformer) {
    private[SparkUtils] def getIfExists(colName: String) = {
      if (transformer.hasParam(colName)) {
        Seq(transformer.get(transformer.getParam(colName)).get)
      } else {
        Seq.empty
      }
    }

    private[SparkUtils] def extractParams(params: Seq[String]): Seq[String] = {
      params
        .flatMap(transformer.getIfExists)
        .map(_.toString)
    }

    def getLabelsColumns: Seq[String] ={
      extractParams(List("labelCol"))
    }

    def getInputColumns: Seq[String] = {
      extractParams(List("inputCol", "featuresCol"))
    }

    def getOutputColumns: Seq[String] = {
      extractParams(List("outputCol", "predictionCol", "probabilityCol", "rawPredictionCol"))
    }
  }

  implicit class PumpedPipelineModel(pipelineModel: PipelineModel) {
    val inputs = pipelineModel.stages.flatMap(_.getInputColumns)
    val outputs = pipelineModel.stages.flatMap(_.getOutputColumns)
    val labels = {
      val labelCols = pipelineModel.stages.flatMap(_.getLabelsColumns)
      val trainModels = pipelineModel.stages.filter(_.getOutputColumns.containsSlice(labelCols))
      trainModels.flatMap(_.getInputColumns)
    }

    def getInputColumns: Seq[String] = {
      inputs.diff(outputs).diff(labels)
    }

    def getOutputColumns: Seq[String] = {
      outputs.diff(inputs)
    }
  }
}
