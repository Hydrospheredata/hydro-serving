package io.hydrosphere.serving.manager.it.service

import cats.data.EitherT
import cats.instances.all._
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import io.hydrosphere.serving.manager.controller.model.ModelUpload
import io.hydrosphere.serving.manager.it.FullIntegrationSpec
import io.hydrosphere.serving.manager.service.model_build.BuildModelRequest
import io.hydrosphere.serving.manager.util.task.ServiceTask.ServiceTaskStatus
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class ModelBuildServiceITSpec extends FullIntegrationSpec with BeforeAndAfterAll {
  implicit val awaitTimeout = 50.seconds

  val upload1 = ModelUpload(
    packModel("/models/dummy_model"),
    name = Some("m1")
  )

  "ModelBuild serivce" should {
    "build a model" in {
      eitherTAssert {
        for {
          model <- EitherT(managerServices.modelManagementService.getModel(1))
          buildRequest = BuildModelRequest(1, None, Some(1))
          build <- EitherT(managerServices.modelBuildManagmentService.buildModel(buildRequest))
          modelVersion <- EitherT.liftF(build.future)
          lastBuilds <- EitherT.liftF(managerServices.modelBuildManagmentService.lastModelBuildsByModelId(model.id, 1))
        } yield {
          val lastBuild = lastBuilds.head
          // check that build is successful
          assert(lastBuild.status == ServiceTaskStatus.Finished)
          assert(lastBuild.finished.isDefined)
          assert(lastBuild.version == 1)

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

    "return last version" when {
      "for all models" in {
        eitherTAssert {
          for {
            v1 <- EitherT(managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(1)))
            _ <- EitherT.liftF(v1.future)
            v2 <- EitherT(managerServices.modelBuildManagmentService.buildModel(BuildModelRequest(1)))
            cv2 <- EitherT.liftF(v2.future)
            versions <- EitherT.liftF(managerServices.aggregatedInfoUtilityService.allModelsAggregatedInfo())
          } yield {
            val maybeModelInfo = versions.find(_.model.id == 1)
            assert(maybeModelInfo.isDefined)
            val modelInfo = maybeModelInfo.get
            assert(modelInfo.lastModelVersion.isDefined)
            val lastModelVersion = modelInfo.lastModelVersion.get
            assert(cv2.modelVersion === lastModelVersion.modelVersion)
          }
        }
      }
    }
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    dockerClient.pull("hydrosphere/serving-runtime-dummy:latest")

    val f = for {
      d1 <- EitherT(managerServices.modelManagementService.uploadModel(upload1))
    } yield {
      d1
    }

    Await.result(f.value, 30 seconds)
  }
}
