package io.hydrosphere.serving.manager.model

trait ModelSource

case class LocalModelSource (
  id: Long,
  name: String,
  path: String
) extends ModelSource

case class S3ModelSource (
  id: Long,
  name: String,
  keyId: String,
  secretKey: String,
  bucketName: String,
  queueName: String,
  region: String
) extends ModelSource