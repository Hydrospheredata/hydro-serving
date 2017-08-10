package io.hydrosphere.serving.manager.service

import java.nio.file.Files

import io.hydrosphere.serving.manager.LocalModelSourceConfiguration
import io.hydrosphere.serving.manager.service.modelsource.LocalModelSource
import org.scalatest.{FlatSpec, Matchers}

class ModelSourceSpecs extends FlatSpec with Matchers  {
  val localSource = new LocalModelSource(LocalModelSourceConfiguration("test", "/Users/bulat/Documents/Dev/Provectus/hydro-serving/manager/src/test/resources/test_models"))

  "LocalModelSource" should "list all directories" in {
    localSource.getSubDirs should contain allElementsOf List("scikit_model", "spark_model", "tensorflow_model")
    localSource.getSubDirs("scikit_model") shouldBe empty
    localSource.getSubDirs("spark_model").toSet shouldBe Set("metadata", "stages")
  }

  it should "list all files " in {
    localSource.getAllFiles("tensorflow_model").toSet should contain allElementsOf Set("saved_model.pb")
  }

  it should "have a correct prefix" in {
    localSource.getSourcePrefix() shouldBe "test"
  }

  it should "return correct absolute path for model" in {
    val path = localSource.getAbsolutePath("spark_model")
    Files.isDirectory(path) shouldBe true
  }

  it should "return readable file" in {
    val file = localSource.getReadableFile("scikit_model/metadata.json")
    file.exists() shouldBe true
    val content = Files.readAllLines(file.toPath)
    content should not be empty
  }


  "S3ModelSource" should "list all directories" in {

  }
}
