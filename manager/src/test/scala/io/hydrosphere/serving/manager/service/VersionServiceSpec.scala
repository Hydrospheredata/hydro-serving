package io.hydrosphere.serving.manager.service

import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.GenericUnitTest
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionRepositoryAlgebra, ModelVersionService, ModelVersionStatus}
import io.hydrosphere.serving.model.api.ModelType

import scala.concurrent.Future

class VersionServiceSpec extends GenericUnitTest {
  describe("ModelVersionService") {
    describe("should calculate the right version") {
      it("for a new model") {
        val versionRepo = mock[ModelVersionRepositoryAlgebra[Future]]
        when(versionRepo.lastModelVersionByModel(1, 1)).thenReturn(
          Future.successful(Seq.empty)
        )
        val versionService = new ModelVersionService(versionRepo,null,null,null,null,null)
        versionService.getNextModelVersion(1).map{ x =>
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
        val versionService = new ModelVersionService(versionRepo,null,null,null,null,null)
        versionService.getNextModelVersion(1).map{ x =>
          assert(x.right.get === 5)
        }
      }
    }
  }
}
