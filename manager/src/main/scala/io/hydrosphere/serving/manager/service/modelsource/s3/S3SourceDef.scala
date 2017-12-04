package io.hydrosphere.serving.manager.service.modelsource.s3

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClientBuilder}
import io.hydrosphere.serving.manager.model.{ModelSourceConfig, S3SourceParams}
import io.hydrosphere.serving.manager.service.modelsource.SourceDef

case class S3SourceDef(
  name: String,
  bucket: String,
  queue: String,
  s3Client: AmazonS3,
  sqsClient: AmazonSQS
) extends SourceDef {
  override def prefix = name
}

object S3SourceDef{
  def fromConfig(s3ModelSourceConfiguration: ModelSourceConfig[S3SourceParams]): S3SourceDef = {
    val s3Params = s3ModelSourceConfiguration.params
    val s3Client = AmazonS3ClientBuilder.standard().withRegion(s3Params.region).build()
    val sqsClient = AmazonSQSClientBuilder.standard().withRegion(s3Params.region).build()
    S3SourceDef(
      s3ModelSourceConfiguration.name,
      s3Params.bucketName,
      s3Params.queueName,
      s3Client,
      sqsClient
    )
  }
}