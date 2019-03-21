package io.hydrosphere.serving.manager.infrastructure.protocol

import io.hydrosphere.serving.manager.domain.application._
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionStatus}
import io.hydrosphere.serving.manager.domain.servable.Servable
import spray.json._


trait ModelJsonProtocol extends CommonJsonProtocol with ContractJsonProtocol {

  implicit val dockerImageFormat = jsonFormat3(DockerImage.apply)

  implicit val modelFormat = jsonFormat2(Model)
  implicit val environmentFormat = jsonFormat3(HostSelector)
  implicit val versionStatusFormat = enumFormat(ModelVersionStatus)
  implicit val modelVersionFormat = jsonFormat13(ModelVersion.apply)
  implicit val serviceFormat = jsonFormat6(Servable.apply)

  implicit val detailedServiceFormat = jsonFormat2(ModelVariant.apply)
  implicit val applicationStageFormat = jsonFormat2(PipelineStage.apply)
  implicit val applicationExecutionGraphFormat = jsonFormat1(ApplicationExecutionGraph)
  implicit val applicationKafkaStreamingFormat = jsonFormat4(ApplicationKafkaStream)
  implicit val appStatusFormat = enumFormat(ApplicationStatus)
  implicit val applicationFormat = jsonFormat7(Application.apply)
}

object ModelJsonProtocol extends ModelJsonProtocol