package io.hydrosphere.serving.manager.infrastructure.protocol

import io.hydrosphere.serving.manager.domain.application._
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.ModelVersion
import io.hydrosphere.serving.manager.domain.service.Service
import io.hydrosphere.serving.model.api.ModelType
import spray.json._


trait ModelJsonProtocol extends CommonJsonProtocol with ContractJsonProtocol {
  implicit val modelTypeFormat = new JsonFormat[ModelType] {
    override def read(json: JsValue) = {
      json match {
        case JsString(str) => ModelType.fromTag(str)
        case x => throw DeserializationException(s"$x is not a valid ModelType")
      }
    }

    override def write(obj: ModelType) = {
      JsString(obj.toTag)
    }
  }

  implicit val dockerImageFormat = jsonFormat3(DockerImage.apply)

  implicit val modelFormat = jsonFormat2(Model)
  implicit val environmentFormat = jsonFormat3(HostSelector)
  implicit val modelVersionFormat = jsonFormat11(ModelVersion.apply)
  implicit val serviceFormat = jsonFormat6(Service.apply)

  implicit val detailedServiceFormat = jsonFormat3(DetailedServiceDescription.apply)
  implicit val applicationStageFormat = jsonFormat2(ApplicationStage.apply)
  implicit val applicationExecutionGraphFormat = jsonFormat1(ApplicationExecutionGraph)
  implicit val applicationKafkaStreamingFormat = jsonFormat4(ApplicationKafkaStream)
  implicit val applicationFormat = jsonFormat6(Application.apply)
}

object ModelJsonProtocol extends ModelJsonProtocol