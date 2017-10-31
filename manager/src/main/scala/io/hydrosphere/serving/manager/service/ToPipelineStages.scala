package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.connector.ExecutionUnit
import io.hydrosphere.serving.model.{ModelService, Pipeline, WeightedService}

trait ToPipelineStages[A] {

  def toStages(a: A, servePath: String) : Seq[ExecutionUnit]

}

object ToPipelineStages {

  implicit val modelToStages = new ToPipelineStages[ModelService] {
    def toStages(model: ModelService, servePath: String) : Seq[ExecutionUnit] = {
      Seq(ExecutionUnit(model.serviceName, servePath))
    }
  }

  implicit val pipelineToStages = new ToPipelineStages[Pipeline] {
    def toStages(pipeline: Pipeline, servePath: String) : Seq[ExecutionUnit] =
      pipeline.stages.map(stage => ExecutionUnit(stage.serviceName, stage.servePath))
  }

  implicit val weightedToStages = new ToPipelineStages[WeightedService] {
    def toStages(s: WeightedService, servePath: String) : Seq[ExecutionUnit] = {
      Seq(ExecutionUnit(s.serviceName, servePath))
    }
  }

  implicit class ToStagesSyntax[A](a: A)(implicit val toSt: ToPipelineStages[A]) {
    def toPipelineStages(servePath: String): Seq[ExecutionUnit] = toSt.toStages(a, servePath)
  }
}

