package io.hydrosphere.serving.manager.service.source.sources

import java.nio.file.Files

import io.hydrosphere.serving.manager.GenericUnitTest

abstract class ModelSourceSpec(val modelSource: ModelSource) extends GenericUnitTest {

  def getSourceFile(path: String): String

  modelSource.getClass.getSimpleName should "list all directories" in {
    assert(modelSource.getSubDirs(getSourceFile("scikit_model")).isRight)

    val sparkFilesResult = modelSource.getSubDirs(getSourceFile("spark_model"))
    assert(sparkFilesResult.isRight)
    val sparkFiles = sparkFilesResult.right.get
    sparkFiles.toSet shouldBe Set("metadata", "stages")
  }

  it should "list all files " in {
    val tfResult = modelSource.getAllFiles(getSourceFile("tensorflow_model"))
    assert(tfResult.isRight, tfResult)
    val tfFiles = tfResult.right.get
    tfFiles.toSet should contain allElementsOf Set("saved_model.pb")
  }

  it should "have a correct prefix" in {
    modelSource.sourceDef.name shouldBe "test"
  }


  it should "return readable file" in {
    val fileResult = modelSource.getReadableFile(getSourceFile("scikit_model/metadata.json"))
    assert(fileResult.isRight, fileResult)
    val file = fileResult.right.get
    file.exists() shouldBe true
    val content = Files.readAllLines(file.toPath)
    content should not be empty
  }
}
