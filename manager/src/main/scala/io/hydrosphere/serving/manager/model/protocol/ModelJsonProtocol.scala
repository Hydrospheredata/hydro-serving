package io.hydrosphere.serving.manager.model.protocol

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db._
import spray.json._


trait ModelJsonProtocol extends CommonJsonProtocol with ContractJsonProtocol {
  implicit val modelBuildStatusFormat = enumFormat(ModelBuildStatus)

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

  implicit val modelFormat = jsonFormat7(Model)
  implicit val runtimeFormat = jsonFormat6(Runtime)
  implicit val modelVersionFormat = jsonFormat10(ModelVersion)
  implicit val environmentFormat = jsonFormat3(Environment)
  implicit val serviceFormat = jsonFormat8(Service)
  implicit val modelBuildFormat = jsonFormat9(ModelBuild)

  implicit val serviceKeyDescriptionFormat = jsonFormat5(ServiceKeyDescription.apply)
  implicit val serviceWeightFormat = jsonFormat3(WeightedService)
  implicit val applicationStageFormat = jsonFormat2(ApplicationStage.apply)
  implicit val applicationExecutionGraphFormat = jsonFormat1(ApplicationExecutionGraph)
  implicit val applicationKafkaStreamingFormat = jsonFormat4(ApplicationKafkaStream)
  implicit val applicationFormat = jsonFormat5(Application)
}

object ModelJsonProtocol extends ModelJsonProtocol