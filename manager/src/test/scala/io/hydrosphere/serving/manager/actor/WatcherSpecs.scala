package io.hydrosphere.serving.manager.actor

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{Files, Paths}
import java.time.{LocalDateTime, ZoneId}

import akka.actor.ActorSystem
import io.hydrosphere.serving.manager.{LocalModelSourceConfiguration, TestConstants}
import io.hydrosphere.serving.manager.actor.modelsource.{LocalSourceWatcher, SourceWatcher}
import io.hydrosphere.serving.manager.service.modelsource.{LocalModelSource, ModelSource}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.google.common.hash.Hashing
import io.hydrosphere.serving.manager.actor.modelsource.SourceWatcher
import scala.concurrent.duration._

class WatcherSpecs extends TestKit(ActorSystem("MySpec")) with ImplicitSender with Matchers with WordSpecLike with BeforeAndAfterAll {

  "LocalSourceWatcher" must {
    "detect creation" in {
      val dir = Files.createTempDirectory("local_watcher")
      val localSource = new LocalModelSource(LocalModelSourceConfiguration("test", dir.toString))
      val watcher = system.actorOf(SourceWatcher.props(localSource))

      val filePath = Paths.get(dir.toString, "creation_test_file")
      val relativePath = localSource.sourceFile.toPath.relativize(filePath)
      Files.deleteIfExists(filePath)

      val creationProbe = TestProbe()
      system.eventStream.subscribe(creationProbe.ref, classOf[SourceWatcher.FileCreated])

      Files.createFile(filePath)
      val hash = com.google.common.io.Files.asByteSource(filePath.toFile).hash(Hashing.sha256()).toString
      val attributes = Files.readAttributes(filePath, classOf[BasicFileAttributes])
      val createTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant, ZoneId.systemDefault())
      creationProbe.expectMsg(3.minute, SourceWatcher.FileCreated(localSource, relativePath.toString, hash, createTime))

      Files.delete(filePath)
    }

    "detect a deletion of a model" in {
      val dir = Files.createTempDirectory("local_watcher")
      val localSource = new LocalModelSource(LocalModelSourceConfiguration("test", dir.toString))
      val watcher = system.actorOf(SourceWatcher.props(localSource))

      val filePath = Paths.get(dir.toString, "deletion_test_file")
      val relativePath = localSource.sourceFile.toPath.relativize(filePath)

      val creationProbe = TestProbe()
      system.eventStream.subscribe(creationProbe.ref, classOf[SourceWatcher.FileCreated])

      Files.deleteIfExists(filePath)
      Files.createFile(filePath)
      println(s"File $filePath created")

      val hash = com.google.common.io.Files.asByteSource(filePath.toFile).hash(Hashing.sha256()).toString
      val attributes = Files.readAttributes(filePath, classOf[BasicFileAttributes])
      val createTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant, ZoneId.systemDefault())
      creationProbe.expectMsg(3.minute, SourceWatcher.FileCreated(localSource, relativePath.toString, hash, createTime))

      val deletionProbe = TestProbe()
      system.eventStream.subscribe(deletionProbe.ref, classOf[SourceWatcher.FileDeleted])

      Files.delete(filePath)
      println(s"File $filePath deleted")

      deletionProbe.expectMsg(3.minute, SourceWatcher.FileDeleted(localSource, relativePath.toString))
    }

    "detect a change of a model" in {

    }
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }


}
