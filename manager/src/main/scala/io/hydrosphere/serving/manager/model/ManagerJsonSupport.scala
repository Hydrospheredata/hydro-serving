package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.controller.BuildModelRequest
import io.hydrosphere.serving.manager.service.{CreateServiceRequest, _}
import spray.json._

/**
  *
  */
trait ManagerJsonSupport extends CommonJsonSupport {
  implicit val modelServiceInstanceStatusFormat = new EnumJsonConverter(ServiceInstanceStatus)


  implicit val buildModelRequestFormat = jsonFormat2(BuildModelRequest)

  implicit val createServiceRequest = jsonFormat5(CreateServiceRequest)

  implicit val createRuntimeRequest = jsonFormat5(CreateRuntimeRequest)

  implicit val createOrUpdateModelRequest = jsonFormat6(CreateOrUpdateModelRequest)

  implicit val createModelVersionRequest = jsonFormat12(CreateModelVersionRequest)

  implicit val applicationCreateOrUpdateRequest = jsonFormat3(ApplicationCreateOrUpdateRequest)

  implicit val awsAuthFormat = jsonFormat2(AWSAuthKeys)

  implicit val localSourceParamsFormat = jsonFormat1(LocalSourceParams)

  implicit val s3SourceParamsFormat = jsonFormat4(S3SourceParams)

  implicit val sourceParamsFormat = new JsonFormat[SourceParams] {
    override def read(json: JsValue) = {
      json match {
        case JsObject(fields) if fields.isDefinedAt("path") =>
          LocalSourceParams(fields("path").convertTo[String])
        case JsObject(fields) if fields.isDefinedAt("queueName") && fields.isDefinedAt("bucketName") =>
          S3SourceParams(
            awsAuth = fields.get("awsAuth").map(_.convertTo[AWSAuthKeys]),
            bucketName = fields("bucketName").convertTo[String],
            queueName = fields("queueName").convertTo[String],
            region = fields("region").convertTo[String]
          )
      }
    }

    override def write(obj: SourceParams) = {
      obj match {
        case x: LocalSourceParams => x.toJson
        case x: S3SourceParams => x.toJson
        case _ => ???
      }
    }
  }

  implicit val modelSourceConfigFormat = jsonFormat3(ModelSourceConfigAux)

  implicit val createModelSourceRequestFormat = jsonFormat2(CreateModelSourceRequest)

  implicit val createEnvironmentRequest = jsonFormat2(CreateEnvironmentRequest)

}