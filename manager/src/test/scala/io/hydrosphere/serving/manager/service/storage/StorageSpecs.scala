package io.hydrosphere.serving.manager.service.storage

import java.nio.file.{Files, Path, Paths}
import java.time.Instant

import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.infrastructure.storage.LocalModelStorage
import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfterAll

class StorageSpecs extends GenericUnitTest with BeforeAndAfterAll {

  var tempDir: Path = _

  describe("Model storage") {

    it("should list all directories") {
      val modelSource = new LocalModelStorage(tempDir)

      assert(modelSource.getSubDirs("scikit_model").isRight)

      val sparkFilesResult = modelSource.getSubDirs("spark_model")
      assert(sparkFilesResult.isRight)
      val sparkFiles = sparkFilesResult.right.get
      sparkFiles.toSet shouldBe Set("metadata", "stages")
    }

    it("should list all files") {
      val modelSource = new LocalModelStorage(tempDir)

      val tfResult = modelSource.getAllFiles("tensorflow_model")
      assert(tfResult.isRight, tfResult)
      val tfFiles = tfResult.right.get
      tfFiles.toSet should contain allElementsOf Set("saved_model.pb")
    }

    it("should return a readable file") {
      val modelSource = new LocalModelStorage(tempDir)

      val fileResult = modelSource.getReadableFile("tensorflow_model/saved_model.pb")
      assert(fileResult.isRight, fileResult)
      val file = fileResult.right.get
      file.exists() shouldBe true
    }

    it("should write a new file") {
      val modelSource = new LocalModelStorage(tempDir)
      val oldFile = modelSource.getReadableFile("tensorflow_model/saved_model.pb").right.get
      val fileRes = modelSource.writeFile("tensorflow_model/new_file.pb", oldFile)
      val file = fileRes.right.get
      assert(Files.exists(file))
    }

    it("should overwrite a file") {
      val modelSource = new LocalModelStorage(tempDir)
      val targetFile = "tensorflow_model/new_file.pb"
      val srcFile = modelSource.getReadableFile("scikit_model/contract.prototxt").right.get

      val prevFile = modelSource.getReadableFile(targetFile).right.get
      val prevLastModified = Files.getLastModifiedTime(prevFile.toPath)

      modelSource.writeFile(targetFile, srcFile)
      val currentFile = modelSource.getReadableFile(targetFile).right.get
      val currentLastModified = Files.getLastModifiedTime(currentFile.toPath)

      assert(currentFile.exists())
      assert(currentLastModified.toMillis != prevLastModified.toMillis)
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    tempDir = Files.createTempDirectory("storage-spec")
    val testModelsDir = Paths.get(getClass.getClassLoader.getResource("test_models").toURI)
    FileUtils.copyDirectory(testModelsDir.toFile, tempDir.toFile)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    FileUtils.deleteDirectory(tempDir.toFile)
  }
}