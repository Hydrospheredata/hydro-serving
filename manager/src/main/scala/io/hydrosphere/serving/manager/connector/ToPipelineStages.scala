package io.hydrosphere.serving.manager.connector

import io.hydrosphere.serving.manager.model.{Application, Service}

trait ToPipelineStages[A] {

  def toStages(a: A, servePath: String): Seq[ExecutionUnit]

}

/**
  * Converts different services to pipeline stages
  */
object ToPipelineStages {

  implicit val modelToStages = new ToPipelineStages[Service] {
    def toStages(service: Service, servePath: String): Seq[ExecutionUnit] = {
      Seq(ExecutionUnit(service.serviceName, servePath))
    }
  }

  implicit val applicationToStages = new ToPipelineStages[Application] {
    def toStages(application: Application, servePath: String): Seq[ExecutionUnit] =
      application.executionGraph.stages.zipWithIndex.map {
        case (stage, idx) => ExecutionUnit(
          serviceName = s"app${application.id}stage$idx",
          servicePath = stage.signatureName
        )
      }
  }

  implicit class ToStagesSyntax[A](a: A)(implicit val toSt: ToPipelineStages[A]) {
    def toPipelineStages(servePath: String): Seq[ExecutionUnit] = toSt.toStages(a, servePath)
  }

}

