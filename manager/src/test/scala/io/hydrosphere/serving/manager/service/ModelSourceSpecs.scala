package io.hydrosphere.serving.manager.service

import java.nio.file.Files

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import io.hydrosphere.serving.manager.service.modelsource.{LocalModelSource, ModelSource, S3ModelSource}
import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, S3ModelSourceConfiguration, TestConstants}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConversions._

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

  "S3Source" should "be ok" in {
      val s3client = AmazonS3ClientBuilder
        .standard
        .withRegion("eu-west-1")
        .build()
      val sqsClient = AmazonSQSClientBuilder
        .standard
        .withRegion("eu-west-1")
        .build
      val s3Source = new S3ModelSource(S3ModelSourceConfiguration(
        "test",
        "",
        Regions.EU_WEST_1,
        s3client,
        sqsClient,
        "serving-demo",
        "serving-s3-repo-queue"
      ))
    val folders = s3Source.getAllFiles("dt4")
    val f = s3Source.getSubDirs("dt4")
    val t = s3Source.client
      .listObjects(s3Source.configuration.bucket)
      .getObjectSummaries
      .map(_.getKey)
      .filter(_.startsWith("dt4/stages/"))
      .map(_.split("dt4/stages/").last.split("/"))
    val a = s3Source.cacheSource.getSubDirs("dt")
    println(folders)
  }


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
