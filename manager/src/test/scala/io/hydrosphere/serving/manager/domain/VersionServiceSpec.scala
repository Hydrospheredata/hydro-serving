package io.hydrosphere.serving.manager.domain

import java.time.LocalDateTime

import cats.effect.IO
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version._

class VersionServiceSpec extends GenericUnitTest {
  describe("ModelVersion service") {
    describe("should calculate the right version") {
      it("for a new model") {
        ioAssert {
          val versionRepo = mock[ModelVersionRepository[IO]]
          when(versionRepo.lastModelVersionByModel(1, 1)).thenReturn(
            IO(Seq.empty)
          )
          val versionService = ModelVersionService[IO](versionRepo, null)
          versionService.getNextModelVersion(1).map { x =>
            assert(x === 1)
          }
        }
      }

      it("for a built model") {
        ioAssert {
          val versionRepo = mock[ModelVersionRepository[IO]]
          when(versionRepo.lastModelVersionByModel(1, 1)).thenReturn(
            IO(Seq(ModelVersion(
              id = 1,
              image = DockerImage("", ""),
              created = LocalDateTime.now(),
              finished = None,
              modelVersion = 4,
              modelContract = ModelContract.defaultInstance,
              runtime = DockerImage("", ""),
              model = Model(1, "aaaa"),
              hostSelector = None,
              status = ModelVersionStatus.Assembling,
              profileTypes = Map.empty,
              installCommand = None,
              metadata = Map.empty
            )))
          )
          val versionService = ModelVersionService[IO](versionRepo, null)
          versionService.getNextModelVersion(1).map { x =>
            assert(x === 5)
          }
        }
      }
    }
  }
}