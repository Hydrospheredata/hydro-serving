package io.hydrosphere.serving.manager.it.service

import java.nio.file.Path

import cats.data.EitherT
import cats.effect.IO
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.ContainerConfig
import io.hydrosphere.serving.manager.api.http.controller.model.ModelUploadMetadata
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model_version.ModelVersionStatus
import io.hydrosphere.serving.manager.it.FullIntegrationSpec
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._

class ModelBuildITSpec extends FullIntegrationSpec {
  private implicit val awaitTimeout: FiniteDuration = 50.seconds

  private val uploadFile: Path = packModel("/models/dummy_model")
  private val uploadMetadata = ModelUploadMetadata(
    name = "the-best-model-in-the-world",
    runtime = DockerImage(
      name = "hydrosphere/serving-runtime-dummy",
      tag = "latest"
    )
  )

  describe("Model serivce") {
    it("should create an image with correct name and content") {
      eitherTAssert {
        for {
          models <- EitherT.liftF(managerRepositories.modelVersionRepository.all())
          mv <- EitherT(managerServices.modelService.uploadModel(uploadFile, uploadMetadata))
          builtMv <- EitherT.liftF(mv.completedVersion.get)
        } yield {
          println(models)
          println("BUILD_RESULT:")
          println(builtMv)
          // check that build is successful
          assert(builtMv.status === ModelVersionStatus.Released)
          assert(builtMv.finished.isDefined)
          assert(builtMv.model.name === "the-best-model-in-the-world")
          assert(builtMv.modelVersion === 1)
          assert(builtMv.image.name === "the-best-model-in-the-world")
          assert(builtMv.image.tag === "1")

          // check files in container
          val config = ContainerConfig.builder()
            .image(builtMv.image.fullName)
            .cmd("sh", "-c", "while :; do sleep 1; done")
            .build()
          val container = dockerClient.createContainer(
            config,
            s"test-${builtMv.model.name}"
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
          val actualLogs = dockerClient.execStart(exec.id()).readFully().split("\n").toSet
          val expectedLogs = Set(
            "/model",
            "/model/contract.protobin",
            "/model/files",
            "/model/files/stages",
            "/model/files/stages/0_w2v_4c3ed7c223c6",
            "/model/files/stages/0_w2v_4c3ed7c223c6/data",
            "/model/files/stages/0_w2v_4c3ed7c223c6/data/_SUCCESS",
            "/model/files/stages/0_w2v_4c3ed7c223c6/data/.part-00000-2624bff2-e5d7-46c9-9ec3-42178b8a4e68.snappy.parquet.crc",
            "/model/files/stages/0_w2v_4c3ed7c223c6/data/part-00000-2624bff2-e5d7-46c9-9ec3-42178b8a4e68.snappy.parquet",
            "/model/files/stages/0_w2v_4c3ed7c223c6/metadata",
            "/model/files/stages/0_w2v_4c3ed7c223c6/metadata/_SUCCESS",
            "/model/files/stages/0_w2v_4c3ed7c223c6/metadata/part-00000",
            "/model/files/stages/0_w2v_4c3ed7c223c6/metadata/.part-00000.crc",
            "/model/files/metadata",
            "/model/files/metadata/_SUCCESS",
            "/model/files/metadata/part-00000",
            "/model/files/metadata/.part-00000.crc"
          )
          println(actualLogs.mkString(compat.Platform.EOL))
          assert(actualLogs.subsetOf(expectedLogs))
        }
      }
    }
  }
}