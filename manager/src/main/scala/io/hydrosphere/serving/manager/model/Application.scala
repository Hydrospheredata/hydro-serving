package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

case class Application(
  id: Long,
  name: String,
  executionGraph: ApplicationExecutionGraph,
  sourcesList: List[Long]
)

object Application {
  implicit val applicationFormat = jsonFormat4(Application.apply)
}