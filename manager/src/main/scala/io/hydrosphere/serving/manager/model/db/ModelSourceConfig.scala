package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.SourceParams

case class ModelSourceConfig(
  id: Long,
  name: String,
  params: SourceParams
)

object ModelSourceConfig {
  trait SourceParams

  case class LocalSourceParams(
    pathPrefix: Option[String]
  ) extends SourceParams

  case class AWSAuthKeys(keyId: String, secretKey: String) {
    def hide: AWSAuthKeys = AWSAuthKeys("***************", "***************")
  }

  case class S3SourceParams (
    awsAuth: Option[AWSAuthKeys],
    bucketName: String,
    path: String,
    region: String
  ) extends SourceParams
}