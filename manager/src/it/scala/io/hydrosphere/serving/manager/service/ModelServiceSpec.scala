package io.hydrosphere.serving.manager.service

import akka.testkit.TestProbe
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import io.hydrosphere.serving.manager.model.{LocalSourceParams, ModelBuildStatus, ModelSourceConfig}
import io.hydrosphere.serving.manager.service.actors.RepositoryIndexActor
import io.hydrosphere.serving.manager.test.FullIntegrationSpec
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Future
import scala.concurrent.duration._

class ModelServiceSpec extends FullIntegrationSpec with BeforeAndAfterAll {
  "ModelService" should {
    "fetch all models" in {
      managerServices.modelManagementService.allModels().map { seq =>
        assert(seq.lengthCompare(1) == 0)
      }
    }

    "build a modelversion" in {
      managerRepositories.modelRepository.get(1).flatMap {
        case None => Future.failed(new IllegalArgumentException("Model is not found"))
        case Some(model) =>
          managerServices.modelManagementService.buildModel(model.id, Some(1)).flatMap { _ =>
            Thread.sleep(1000)
            managerServices.modelManagementService.lastModelBuildsByModelId(model.id, 1).map { lastBuilds =>
              val lastBuild = lastBuilds.head
              // check that build is successful
              assert(lastBuild.status == ModelBuildStatus.FINISHED)
              assert(lastBuild.finished.isDefined)
              assert(lastBuild.version == 1)
              val modelVersion = lastBuild.modelVersion.get

              // check files in container
              val config = ContainerConfig.builder()
                .image(modelVersion.toImageDef)
                .cmd("sh", "-c", "while :; do sleep 1; done")
                .build()
              val container = dockerClient.createContainer(
                config,
                s"test-${modelVersion.modelName}"
              )
              Thread.sleep(500)
              dockerClient.startContainer(container.id())

              Thread.sleep(500)
              val exec = dockerClient.execCreate(
                container.id(),
                Array("find", "/model"),
                DockerClient.ExecCreateParam.attachStdin(),
                DockerClient.ExecCreateParam.attachStdout()
              )
              val logs = dockerClient.execStart(exec.id()).readFully().split("\n").toSet
              val expected =
                """/model
                  |/model/contract.protobin
                  |/model/files
                  |/model/files/stages
                  |/model/files/stages/0_w2v_4c3ed7c223c6
                  |/model/files/stages/0_w2v_4c3ed7c223c6/data
                  |/model/files/stages/0_w2v_4c3ed7c223c6/data/_SUCCESS
                  |/model/files/stages/0_w2v_4c3ed7c223c6/data/.part-00000-2624bff2-e5d7-46c9-9ec3-42178b8a4e68.snappy.parquet.crc
                  |/model/files/stages/0_w2v_4c3ed7c223c6/data/part-00000-2624bff2-e5d7-46c9-9ec3-42178b8a4e68.snappy.parquet
                  |/model/files/stages/0_w2v_4c3ed7c223c6/metadata
                  |/model/files/stages/0_w2v_4c3ed7c223c6/metadata/_SUCCESS
                  |/model/files/stages/0_w2v_4c3ed7c223c6/metadata/part-00000
                  |/model/files/stages/0_w2v_4c3ed7c223c6/metadata/.part-00000.crc
                  |/model/files/metadata
                  |/model/files/metadata/_SUCCESS
                  |/model/files/metadata/part-00000
                  |/model/files/metadata/.part-00000.crc""".stripMargin.split("\n").toSet
              println(logs)
              assert(logs.subsetOf(expected))
            }
          }
      }
    }

    "return last version" when {
      "for all models" in {
        for {
          _ <- managerServices.modelManagementService.buildModel(1, None)
          v2 <- managerServices.modelManagementService.buildModel(1, None)
          versions <- managerServices.modelManagementService.allModelsAggregatedInfo()
        } yield {
          val maybeModelInfo = versions.find(_.model.id == 1)
          assert(maybeModelInfo.isDefined)
          val modelInfo = maybeModelInfo.get
          assert(modelInfo.lastModelVersion.isDefined)
          val lastModelVersion = modelInfo.lastModelVersion.get
          assert(v2.modelVersion === lastModelVersion.modelVersion)
        }
      }
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    val indexProbe = TestProbe()
    system.eventStream.subscribe(indexProbe.ref, classOf[RepositoryIndexActor.IndexFinished])
    managerServices.sourceManagementService.addSource(
      ModelSourceConfig(1, "itsource", LocalSourceParams(getClass.getResource("/models").getPath)).toAux
    )
    indexProbe.expectMsg(15.seconds, RepositoryIndexActor.IndexFinished("dummy_model", "itsource"))
  }
}
