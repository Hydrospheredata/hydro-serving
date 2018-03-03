package io.hydrosphere.serving.manager.service.modelsource.s3

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
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
) extends SourceDef

object S3SourceDef {
  def fromConfig(s3ModelSourceConfiguration: ModelSourceConfig[S3SourceParams]): S3SourceDef = {
    val params = s3ModelSourceConfiguration.params
    val s3Client = AmazonS3ClientBuilder.standard().withRegion(params.region)
    val sqsClient = AmazonSQSClientBuilder.standard().withRegion(params.region)
    params.awsAuth.foreach { auth =>
      val authProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(auth.keyId, auth.secretKey))
      s3Client.withCredentials(authProvider)
      sqsClient.withCredentials(authProvider)
    }
    S3SourceDef(
      s3ModelSourceConfiguration.name,
      params.bucketName,
      params.queueName,
      s3Client.build(),
      sqsClient.build()
    )
  }
}