package io.hydrosphere.serving.manager.service.source.sources

import java.nio.file.Files

import io.hydrosphere.serving.manager.GenericUnitTest

abstract class ModelSourceSpec(val modelSource: ModelSource) extends GenericUnitTest {

  def getSourceFile(path: String): String

  modelSource.getClass.getSimpleName should "list all directories" in {
    modelSource.getSubDirs(getSourceFile("scikit_model")) shouldBe empty
    modelSource.getSubDirs(getSourceFile("spark_model")).toSet shouldBe Set("metadata", "stages")
  }

  it should "list all files " in {
    modelSource.getAllFiles(getSourceFile("tensorflow_model")).toSet should contain allElementsOf Set("saved_model.pb")
  }

  it should "have a correct prefix" in {
    modelSource.sourceDef.name shouldBe "test"
  }

  it should "return correct absolute path for model" in {
    val path = modelSource.getAbsolutePath(getSourceFile("spark_model"))
    Files.isDirectory(path) shouldBe true
  }

  it should "return readable file" in {
    val file = modelSource.getReadableFile(getSourceFile("scikit_model/metadata.json"))
    file.exists() shouldBe true
    val content = Files.readAllLines(file.toPath)
    content should not be empty
  }
}
