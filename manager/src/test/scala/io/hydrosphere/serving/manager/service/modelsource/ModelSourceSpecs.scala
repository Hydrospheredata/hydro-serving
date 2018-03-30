package io.hydrosphere.serving.manager.service.modelsource

import java.nio.file.Files
import io.hydrosphere.serving.manager.service.modelsource.local.{LocalModelSource, LocalSourceDef}
import io.hydrosphere.serving.manager.util.FileUtils
import org.scalatest.{FlatSpec, Matchers}

class ModelSourceSpecs extends FlatSpec with Matchers {
  def test(modelSource: ModelSource) = {
    modelSource.getClass.getSimpleName should "list all directories" in {
      modelSource.getSubDirs(FileUtils.getResourcePath("scikit_model")) shouldBe empty
      modelSource.getSubDirs(FileUtils.getResourcePath("spark_model")).toSet shouldBe Set("metadata", "stages")
    }

    it should "list all files " in {
      modelSource.getAllFiles("tensorflow_model").toSet should contain allElementsOf Set("saved_model.pb")
    }

    it should "have a correct prefix" in {
      modelSource.sourceDef.name shouldBe "test"
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

  val localSource = new LocalModelSource(LocalSourceDef("test"))
  test(localSource)

//  "S3Source" should "be ok" in {
//    val s3client = AmazonS3ClientBuilder
//      .standard
//      .withRegion("eu-west-1")
//      .build()
//    val sqsClient = AmazonSQSClientBuilder
//      .standard
//      .withRegion("eu-west-1")
//      .build
//    val s3Source = new S3ModelSource(S3SourceDef(
//      "test",
//      "serving-demo",
//      "serving-s3-repo-queue",
//      s3client,
//      sqsClient
//    ))
//    val folders = s3Source.getAllFiles("dt4")
//    val t = s3Source.sourceDef.s3Client
//      .listObjects(s3Source.sourceDef.bucket)
//      .getObjectSummaries
//      .map(_.getKey)
//      .filter(_.startsWith("dt4/stages/"))
//      .map(_.split("dt4/stages/").last.split("/"))
//    val a = s3Source.cacheSource.getSubDirs("dt")
//    println(folders)
//  }
}
