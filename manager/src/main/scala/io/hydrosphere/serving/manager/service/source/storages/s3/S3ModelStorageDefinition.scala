package io.hydrosphere.serving.manager.service.source.storages.s3

import com.amazonaws.services.s3.AmazonS3
import io.hydrosphere.serving.manager.service.source.storages.ModelStorageDefinition

case class S3ModelStorageDefinition(
  name: String,
  bucket: String,
  path: String,
  s3Client: AmazonS3
) extends ModelStorageDefinition
