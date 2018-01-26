package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

trait SourceParams

object SourceParams {
  import spray.json._

  case class LocalSourceParams (
    path: String
  ) extends SourceParams

  object LocalSourceParams {
    implicit val localSourceParamsFormat: JsonFormat[LocalSourceParams] = jsonFormat1(LocalSourceParams.apply)
  }

  case class AWSAuthKeys(keyId: String, secretKey: String) {
    def hide: AWSAuthKeys = AWSAuthKeys("***************", "***************")
  }

  object AWSAuthKeys {
    implicit val awsAuthFormat: JsonFormat[AWSAuthKeys] = jsonFormat2(AWSAuthKeys.apply)
  }

  case class S3SourceParams (
    awsAuth: Option[AWSAuthKeys],
    bucketName: String,
    queueName: String,
    region: String
  ) extends SourceParams

  object S3SourceParams {
    implicit val s3SourceParamsFormat: JsonFormat[S3SourceParams] = jsonFormat4(S3SourceParams.apply)
  }

  implicit val sourceParamsFormat = new JsonFormat[SourceParams] {
    override def read(json: JsValue) = {
      json match {
        case x @ JsObject(fields) if fields.isDefinedAt("path") =>
          x.convertTo[LocalSourceParams]
        case x @ JsObject(fields) if fields.isDefinedAt("queueName") && fields.isDefinedAt("bucketName") =>
          x.convertTo[S3SourceParams]
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
}