package io.hydrosphere.serving.manager.domain

import cats.data.StateT
import cats.effect.IO
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.domain.image.{DockerImage, ImageBuilder, ImageRepository}
import io.hydrosphere.serving.manager.domain.model.{Model, ModelVersionMetadata}
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionRepository, ModelVersionService}
import org.mockito.Matchers

class ModelVersionBuilderSpec extends GenericUnitTest {
  describe("ModelVersionBuilder") {
    it("should push the built image") {
      ???
//      ioAssert {
//        type StateIO[T] = StateT[IO, List[DockerImage], T]
//        val model = Model(1, "push-me")
//
//        val versionRepo = new ModelVersionRepository[StateIO] {
//          override def create(entity: ModelVersion): StateIO[ModelVersion] = StateT.pure(entity)
//          override def lastModelVersionByModel(modelId: Long, max: Int): StateIO[Seq[ModelVersion]] = StateT.pure(Seq.empty)
//          override def update(id: Long, entity: ModelVersion): StateIO[Int] = ???
//          override def get(id: Long): StateIO[Option[ModelVersion]] = ???
//          override def get(modelName: String, modelVersion: Long): StateIO[Option[ModelVersion]] = ???
//          override def get(idx: Seq[Long]): StateIO[Seq[ModelVersion]] = ???
//          override def delete(id: Long): StateIO[Int] = ???
//          override def all(): StateIO[Seq[ModelVersion]] = ???
//          override def listForModel(modelId: Long): StateIO[Seq[ModelVersion]] = ???
//          override def modelVersionsByModelVersionIds(modelVersionIds: Set[Long]): StateIO[Seq[ModelVersion]] = ???
//        }
//
//        val modelVersionMetadata = ModelVersionMetadata(
//          modelName = model.name,
//          contract = ModelContract.defaultInstance,
//          profileTypes = Map.empty,
//          runtime = DockerImage("run", "time"),
//          hostSelector = None
//        )
//
//        val imageBuilder = mock[ImageBuilder[StateIO]]
//        when(imageBuilder.build(Matchers.any(), Matchers.any())).thenReturn(
//          StateT.pure("random-sha")
//        )
//
//        val imageRepo = new ImageRepository[StateIO] {
//          override def getImage(name: String, tag: String): DockerImage = DockerImage(name, tag)
//
//          override def push(dockerImage: DockerImage): StateIO[Unit] = {
//            StateT.apply { x =>
//              IO((x :+ dockerImage, ()))
//            }
//          }
//        }
//        val versionService = ModelVersionService[StateIO](
//          modelVersionRepository = versionRepo,
//          modelFilePacker = null,
//          imageBuilder = imageBuilder,
//          imageRepository = imageRepo,
//          applicationRepo = null
//        )
//        for {
//          stateful <- versionService.build(model, modelVersionMetadata).run(List.empty)
//          x = stateful._2
//          completedBuild <- IO.fromFuture(IO(x.completedVersion))
//        } yield {
//          val startedBuild = x.startedVersion
//          assert(startedBuild.model.name === "push-me")
//          assert(startedBuild.modelVersion === 1)
//          assert(startedBuild.image === DockerImage("push-me", "1"))
//
//          val images = stateful._1
//          assert(images.lengthCompare(1) === 0 )
//          assert(startedBuild.model.name === completedBuild.model.name)
//          assert(startedBuild.modelVersion === completedBuild.modelVersion)
//          assert(DockerImage("push-me", "1", Some("random-sha-string")) === completedBuild.image)
//        }
//      }
    }
  }
}
