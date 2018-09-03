package io.hydrosphere.serving.manager.model.protocol

import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.manager.model.db._
import io.hydrosphere.serving.manager.service.model.CreateModelRequest
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

  implicit val modelFormat = jsonFormat8(Model)
  implicit val runtimeFormat = jsonFormat6(Runtime)
  implicit val modelVersionFormat = jsonFormat11(ModelVersion)
  implicit val modelBuildFormat = jsonFormat10(ModelBuild.apply)
  implicit val environmentFormat = jsonFormat3(Environment)
  implicit val serviceFormat = jsonFormat8(Service)
  implicit val createModelFormat = jsonFormat4(CreateModelRequest)

  implicit val detailedServiceFormat = jsonFormat5(DetailedServiceDescription.apply)
  implicit val serviceKeyDescriptionFormat = jsonFormat3(ServiceKeyDescription.apply)
  implicit val applicationStageFormat = jsonFormat3(ApplicationStage.apply)
  implicit val applicationExecutionGraphFormat = jsonFormat1(ApplicationExecutionGraph)
  implicit val applicationKafkaStreamingFormat = jsonFormat4(ApplicationKafkaStream)
  implicit val applicationFormat = jsonFormat6(Application.apply)
}

object ModelJsonProtocol extends ModelJsonProtocol