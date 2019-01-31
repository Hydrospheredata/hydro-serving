//package io.hydrosphere.serving.manager.storage
//
//import java.io.PrintWriter
//import java.nio.file._
//
//import cats.effect.IO
//import io.hydrosphere.serving.manager.GenericUnitTest
//import io.hydrosphere.serving.manager.infrastructure.storage.{LocalModelStorage, StorageOps}
//import org.apache.commons.io.FileUtils
//import org.scalatest.BeforeAndAfterAll
//
//class StorageOpsSpecs extends GenericUnitTest with BeforeAndAfterAll {
//  var tempDir: Path = _
//
//  describe("LocalStorageSpecs") {
//
//    it("should return a readable file") {
//      ioAssert {
//        val ops = StorageOps.default[IO]
//        ops.getReadableFile(tempDir.resolve("tensorflow_model/saved_model.pb")).map { fileResult =>
//          assert(fileResult.isDefined, fileResult)
//          val file = fileResult.get
//          file.exists() shouldBe true
//        }
//      }
//    }
//
//    it("should write a new file") {
//      ioAssert {
//        val ops = StorageOps.default[IO]
//        for {
//          oldFile <- ops.getReadableFile(tempDir.resolve("tensorflow_model/saved_model.pb"))
//          f = oldFile.get
//          fileRes <- ops.copyFile(f.toPath, tempDir.resolve("tensorflow_model/new_file.pb"))
//        } yield {
//          assert(Files.exists(fileRes))
//        }
//      }
//    }
//
//    it("should overwrite a file") {
//      val modelSource = new LocalModelStorage(tempDir, ops)
//      val newFile = Files.createTempFile("test", "test")
//      val writer = new PrintWriter(newFile.toFile)
//      writer.write("Never gonna give you up")
//      writer.close()
//      val targetFile = "tensorflow_model/new_file.pb"
//
//      val prevFile = modelSource.getReadableFile(targetFile).right.get
//      val prevLastModified = Files.getLastModifiedTime(prevFile.toPath)
//
//      modelSource.copyFile(targetFile, srcFile)
//      val currentFile = modelSource.getReadableFile(targetFile).right.get
//      val currentLastModified = Files.getLastModifiedTime(currentFile.toPath)
//
//      assert(currentFile.exists())
//      assert(currentLastModified.toMillis != prevLastModified.toMillis)
//    }
//
//    it("should list all directories") {
//      ioAssert {
//        val ops = StorageOps.default[IO]
//        val result = ops.getSubDirs(tempDir.resolve("spark_model"))
//        result.map { maybeFiles =>
//          assert(maybeFiles.isDefined, maybeFiles)
//          val files = maybeFiles.get
//          assert(files.toSet === Set("metadata", "stages"))
//        }
//      }
//    }
//
//    it("should list all files") {
//      ioAssert {
//        val ops = StorageOps.default[IO]
//        ops.getAllFiles(tempDir.resolve("tensorflow_model")).map { tfResult =>
//          assert(tfResult.isDefined, tfResult)
//          val tfFiles = tfResult.get
//          assert(tfFiles.toSet.subsetOf(Set("saved_model.pb")))
//        }
//      }
//    }
//  }
//
//  override def beforeAll(): Unit = {
//    super.beforeAll()
//    tempDir = Files.createTempDirectory("local-storage-ops")
//    val testModelsDir = Paths.get(getClass.getClassLoader.getResource("test_models").toURI)
//    FileUtils.copyDirectory(testModelsDir.toFile, tempDir.toFile)
//  }
//
//  override def afterAll(): Unit = {
//    super.afterAll()
//    FileUtils.deleteDirectory(tempDir.toFile)
//  }
//}
//
//class ModelStorageSpecs extends GenericUnitTest with BeforeAndAfterAll {
//
//  var tempDir: Path = _
//
//  describe("LocalModelStorage") {
//    val ops = mock[StorageOps[IO]]
//
//    it("should unpack tarball") {
//      val modelSource = new LocalModelStorage(tempDir, ops)
//      modelSource.unpack(???, ???)
//    }
//
//    it("should rename folder") {
//      val modelSource = new LocalModelStorage(tempDir, ops)
//      modelSource.rename(???, ???)
//    }
//    it("should write to file") {
//      val modelSource = new LocalModelStorage(tempDir, ops)
//      modelSource.writeFile()
//    }
//
//    it("map storage path to FS") {
//      val modelSource = new LocalModelStorage(tempDir, ops)
//      modelSource.getLocalPath(???)
//    }
//
//  }
//
//  override def beforeAll(): Unit = {
//    super.beforeAll()
//    tempDir = Files.createTempDirectory("storage-spec")
//    val testModelsDir = Paths.get(getClass.getClassLoader.getResource("test_models").toURI)
//    FileUtils.copyDirectory(testModelsDir.toFile, tempDir.toFile)
//  }
//
//  override def afterAll(): Unit = {
//    super.afterAll()
//    FileUtils.deleteDirectory(tempDir.toFile)
//  }
//}