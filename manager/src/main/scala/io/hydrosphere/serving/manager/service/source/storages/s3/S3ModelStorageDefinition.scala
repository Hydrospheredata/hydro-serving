package io.hydrosphere.serving.manager.service.source.storages.s3

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.S3SourceParams
import io.hydrosphere.serving.manager.service.source.storages.ModelStorageDefinition

case class S3ModelStorageDefinition(
  name: String,
  bucket: String,
  path: String,
  s3Client: AmazonS3
) extends ModelStorageDefinition

object S3ModelStorageDefinition {
  def fromConfig(name: String, params: S3SourceParams): S3ModelStorageDefinition = {
    val s3Client = AmazonS3ClientBuilder.standard().withRegion(params.region)
    params.awsAuth.foreach { auth =>
      val authProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(auth.keyId, auth.secretKey))
      s3Client.setCredentials(authProvider)
    }
    S3ModelStorageDefinition(
      name,
      params.bucketName,
      params.path,
      s3Client.build()
    )
  }
}