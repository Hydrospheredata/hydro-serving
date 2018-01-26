package io.hydrosphere.serving.manager.service.management.application

import io.hydrosphere.serving.manager.util.CommonJsonSupport._
import io.hydrosphere.serving.manager.model.{Application, ApplicationExecutionGraph}

case class ApplicationCreateOrUpdateRequest(
  id: Option[Long],
  serviceName: String,
  executionGraph: ApplicationExecutionGraph,
  sourcesList: Option[List[Long]]
) {

  def toApplication: Application = {
    Application(
      id = this.id.getOrElse(0),
      name = this.serviceName,
      executionGraph = this.executionGraph,
      sourcesList = this.sourcesList.getOrElse(List())
    )
  }
}

object ApplicationCreateOrUpdateRequest {
  implicit val applicationCreateOrUpdateRequest = jsonFormat4(ApplicationCreateOrUpdateRequest.apply)
}