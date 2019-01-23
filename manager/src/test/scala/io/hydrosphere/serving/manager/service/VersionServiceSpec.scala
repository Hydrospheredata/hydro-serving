package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import com.spotify.docker.client.ProgressHandler
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.domain.build_script.BuildScriptServiceAlg
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorServiceAlg
import io.hydrosphere.serving.manager.domain.image.{DockerImage, ImageBuilder, ImageRepository}
import io.hydrosphere.serving.manager.domain.model.{Model, ModelVersionMetadata}
import io.hydrosphere.serving.manager.domain.model_version._
import io.hydrosphere.serving.manager.infrastructure.model_build.push.LocalModelPushService
import io.hydrosphere.serving.model.api.{ModelType, Result}
import org.mockito.Matchers

import scala.concurrent.Future

class VersionServiceSpec extends GenericUnitTest {
  describe("ModelVersion service") {
    describe("should calculate the right version") {
      it("for a new model") {
        val versionRepo = mock[ModelVersionRepositoryAlgebra[Future]]
        when(versionRepo.lastModelVersionByModel(1, 1)).thenReturn(
          Future.successful(Seq.empty)
        )
        val versionService = new ModelVersionService(versionRepo, null, null, null, null, null)
        versionService.getNextModelVersion(1).map { x =>
          assert(x.right.get === 1)
        }
      }

      it("for a built model") {
        val versionRepo = mock[ModelVersionRepositoryAlgebra[Future]]
        when(versionRepo.lastModelVersionByModel(1, 1)).thenReturn(
          Future.successful(Seq(ModelVersion(
            id = 1,
            image = DockerImage("", ""),
            created = LocalDateTime.now(),
            finished = None,
            modelVersion = 4,
            modelType = ModelType.Unknown(),
            modelContract = ModelContract.defaultInstance,
            runtime = DockerImage("", ""),
            model = Model(1, "aaaa"),
            hostSelector = None,
            status = ModelVersionStatus.Started,
            profileTypes = Map.empty
          )))
        )
        val versionService = new ModelVersionService(versionRepo, null, null, null, null, null)
        versionService.getNextModelVersion(1).map { x =>
          assert(x.right.get === 5)
        }
      }
    }

    it("should push the built image") {
      val model = Model(1, "push-me")
      val modelType = ModelType.Spark("2.2.0")

      val versionRepo = new ModelVersionRepositoryAlgebra[Future] {
        override def create(entity: ModelVersion): Future[ModelVersion] = Future.successful(entity)

        override def update(id: Long, entity: ModelVersion): Future[Int] = Future.successful(1)

        override def get(id: Long): Future[Option[ModelVersion]] = ???

        override def get(modelName: String, modelVersion: Long): Future[Option[ModelVersion]] = ???

        override def get(idx: Seq[Long]): Future[Seq[ModelVersion]] = ???

        override def delete(id: Long): Future[Int] = ???

        override def all(): Future[Seq[ModelVersion]] = ???

        override def listForModel(modelId: Long): Future[Seq[ModelVersion]] = ???

        override def lastModelVersionByModel(modelId: Long, max: Int): Future[Seq[ModelVersion]] = Future.successful(Seq.empty)

        override def modelVersionsByModelVersionIds(modelVersionIds: Set[Long]): Future[Seq[ModelVersion]] = ???
      }

      val modelVersionMetadata = ModelVersionMetadata(
        modelName = model.name,
        modelType = modelType,
        contract = ModelContract.defaultInstance,
        profileTypes = Map.empty,
        runtime = DockerImage("run", "time"),
        hostSelector = None
      )

      val buildScriptService = mock[BuildScriptServiceAlg]
      when(buildScriptService.fetchScriptForModelType(Matchers.any())).thenReturn(Future.successful("buildscript"))

      val selectorService = mock[HostSelectorServiceAlg]
      val buildService = mock[ImageBuilder]
      when(buildService.build(Matchers.any(), Matchers.any())).thenReturn(Result.okF("random-sha-string"))

      var maybeModelVersion = Option.empty[ModelVersion]
      val pushService = new ImageRepository {
        override def getImage(modelName: String, modelVersion: Long): DockerImage = DockerImage(modelName, modelVersion.toString)

        override def push(modelVersion: ModelVersion, progressHandler: ProgressHandler): Unit = {
          maybeModelVersion = Some(modelVersion)
        }
      }
      val versionService = new ModelVersionService(versionRepo, buildScriptService, selectorService, buildService, pushService, null)
      versionService.build(model, modelVersionMetadata).flatMap { x =>
        assert(x.isRight, x)
        val startedBuild = x.right.get.startedVersion
        assert(startedBuild.model.name === "push-me")
        assert(startedBuild.modelVersion === 1)
        assert(startedBuild.image === DockerImage("push-me", "1"))
        val completedBuildResult = x.right.get.completedVersion
        completedBuildResult.map { completedBuild =>
          assert(maybeModelVersion.isDefined)
          assert(startedBuild.model.name === completedBuild.model.name)
          assert(startedBuild.modelVersion === completedBuild.modelVersion)
          assert(DockerImage("push-me", "1", Some("random-sha-string")) === completedBuild.image)
        }
      }
    }
  }
}