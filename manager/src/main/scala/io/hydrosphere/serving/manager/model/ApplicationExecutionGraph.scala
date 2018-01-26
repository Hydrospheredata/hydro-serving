package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

case class ApplicationExecutionGraph(
  stages: List[ApplicationStage]
)

object ApplicationExecutionGraph {
  implicit val applicationExecutionGraphFormat = jsonFormat1(ApplicationExecutionGraph.apply)
}
