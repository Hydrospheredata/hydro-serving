package io.hydrosphere.serving.manager.connector

import io.hydrosphere.serving.manager.model.{Application, ModelService}

trait ToPipelineStages[A] {

  def toStages(a: A, servePath: String): Seq[ExecutionUnit]

}

/**
  * Converts different services to pipeline stages
  */
object ToPipelineStages {

  implicit val modelToStages = new ToPipelineStages[ModelService] {
    def toStages(model: ModelService, servePath: String): Seq[ExecutionUnit] = {
      Seq(ExecutionUnit(model.serviceName, servePath))
    }
  }

  implicit val applicationToStages = new ToPipelineStages[Application] {
    def toStages(application: Application, servePath: String): Seq[ExecutionUnit] =
      application.executionGraph.stages.indices.map(stage => ExecutionUnit(
        serviceName = s"app${application.id}stage$stage",
        servicePath = "/serve"
      ))
  }

  implicit class ToStagesSyntax[A](a: A)(implicit val toSt: ToPipelineStages[A]) {
    def toPipelineStages(servePath: String): Seq[ExecutionUnit] = toSt.toStages(a, servePath)
  }

}

