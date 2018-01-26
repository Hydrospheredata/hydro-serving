package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

case class ApplicationStage(
  services: List[ServiceWeight],
  signatureName: String
)

object ApplicationStage {
  implicit val applicationStageFormat = jsonFormat2(ApplicationStage.apply)
}