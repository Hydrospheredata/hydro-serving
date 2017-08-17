package io.hydrosphere.serving.manager.service

import java.nio.file.Files

import io.hydrosphere.serving.manager.service.modelsource.{LocalModelSource, ModelSource}
import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, TestConstants}
import org.scalatest.{FlatSpec, Matchers}

class ModelSourceSpecs extends FlatSpec with Matchers {
  def test(modelSource: ModelSource) = {
    modelSource.getClass.getSimpleName should "list all directories" in {
      modelSource.getSubDirs should contain allElementsOf List("scikit_model", "spark_model", "tensorflow_model")
      modelSource.getSubDirs("scikit_model") shouldBe empty
      modelSource.getSubDirs("spark_model").toSet shouldBe Set("metadata", "stages")
    }

    it should "list all files " in {
      modelSource.getAllFiles("tensorflow_model").toSet should contain allElementsOf Set("saved_model.pb")
    }

    it should "have a correct prefix" in {
      modelSource.getSourcePrefix shouldBe "test"
    }

    it should "return correct absolute path for model" in {
      val path = modelSource.getAbsolutePath("spark_model")
      Files.isDirectory(path) shouldBe true
    }

    it should "return readable file" in {
      val file = modelSource.getReadableFile("scikit_model/metadata.json")
      file.exists() shouldBe true
      val content = Files.readAllLines(file.toPath)
      content should not be empty
    }
  }

  val localSource = new LocalModelSource(LocalModelSourceConfiguration("test", TestConstants.localModelsPath))
  test(localSource)


  // TODO fix mocks
  //  val s3Mock = S3Mock(8089)
  //  val s3Endpoint = new EndpointConfiguration("http://localhost:8089", "us-west-2")
  //  val s3client = AmazonS3ClientBuilder
  //    .standard
  //    .withPathStyleAccessEnabled(true)
  //    .withEndpointConfiguration(s3Endpoint)
  //    .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
  //    .build()
  //
  //  val sqsMock = new SQSService(8081)
  //  val sqsEndpoint = new EndpointConfiguration("http://localhost:8081", "us-west-2")
  //  val sqsClient = AmazonSQSClientBuilder
  //    .standard
  //    .withEndpointConfiguration(sqsEndpoint)
  //    .withCredentials(new AWSStaticCredentialsProvider(new AnonymousAWSCredentials()))
  //    .build
  //
  //  s3Mock.start
  //  sqsMock.start
  //  val s3Source = new S3ModelSource(S3ModelSourceConfiguration(
  //    "test",
  //    "",
  //    s3client,
  //    sqsClient,
  //    "s3source-bucket",
  //    "s3source-queue"
  //  ))
  //
  //  test(s3Source)
  //
  //  s3Mock.stop
  //  sqsMock.shutdown
}
