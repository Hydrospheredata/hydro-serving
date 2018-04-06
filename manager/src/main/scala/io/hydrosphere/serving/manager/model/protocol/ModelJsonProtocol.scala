package io.hydrosphere.serving.manager.model.protocol

import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig._
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

  implicit val modelFormat = jsonFormat8(Model)
  implicit val runtimeFormat = jsonFormat6(Runtime)
  implicit val modelVersionFormat = jsonFormat11(ModelVersion)
  implicit val environmentFormat = jsonFormat3(Environment)
  implicit val serviceFormat = jsonFormat8(Service)
  implicit val modelBuildFormat = jsonFormat9(ModelBuild)

  implicit val serviceKeyDescriptionFormat = jsonFormat5(ServiceKeyDescription.apply)
  implicit val serviceWeightFormat = jsonFormat3(WeightedService)
  implicit val applicationStageFormat = jsonFormat2(ApplicationStage.apply)
  implicit val applicationExecutionGraphFormat = jsonFormat1(ApplicationExecutionGraph)
  implicit val applicationKafkaStreamingFormat = jsonFormat4(ApplicationKafkaStream)
  implicit val applicationFormat = jsonFormat5(Application)

  implicit val awsAuthFormat = jsonFormat2(AWSAuthKeys)
  implicit val localSourceParamsFormat = jsonFormat1(LocalSourceParams)
  implicit val s3SourceParamsFormat = jsonFormat4(S3SourceParams)
  implicit val sourceParamsFormat = new JsonFormat[SourceParams] {
    override def read(json: JsValue) = {
      json match {
        case obj@ JsObject(fields) if fields.isDefinedAt("pathPrefix") =>
          obj.convertTo[LocalSourceParams]
        case obj@ JsObject(fields) if fields.isDefinedAt("bucketName") =>
          obj.convertTo[S3SourceParams]
        case x => throw new DeserializationException(s"Can't read ModelSource: $x")
      }
    }

    override def write(obj: SourceParams) = {
      obj match {
        case x: LocalSourceParams => x.toJson
        case x: S3SourceParams => x.toJson
        case x => throw new SerializationException(s"Can't write ModelSource: $x")
      }
    }
  }

  implicit val modelSourceConfigFormat = jsonFormat3(ModelSourceConfig.apply)
}

object ModelJsonProtocol extends ModelJsonProtocol