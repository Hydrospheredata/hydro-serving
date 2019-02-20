package io.hydrosphere.serving.manager.it.db
import java.time.LocalDateTime

import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.model.Model
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionStatus}
import io.hydrosphere.serving.manager.domain.servable.Servable
import io.hydrosphere.serving.manager.it.FullIntegrationSpec

class DbServableRepositorySpec extends FullIntegrationSpec {
  describe("ServableRepository") {
    it("should create one") {
      ioAssert {
        val model = Model(
          id = 0,
          "moddel"
        )
        for {
          resultModel <- managerRepositories.modelRepository.create(model)
          modelVersion = ModelVersion(
            id = 0,
            image = DockerImage("a", "a"),
            created = LocalDateTime.now(),
            finished = None,
            modelVersion = 1,
            modelContract = ModelContract.defaultInstance,
            runtime = DockerImage("r", "r"),
            model = resultModel,
            hostSelector = None,
            status = ModelVersionStatus.Assembling,
            profileTypes = Map.empty,
            installCommand = None,
            metadata = Map.empty
          )
          resultVersion <- managerRepositories.modelVersionRepository.create(modelVersion)
          servable = Servable(
            id = 0,
            serviceName = "aaaa",
            cloudDriverId = None,
            modelVersion = resultVersion,
            statusText = "Ok",
            configParams = Map.empty
          )
          servableResult <- managerRepositories.servableRepository.create(servable)
        } yield {
          println(servableResult)
          assert(servableResult.id === 1)
        }
      }
    }
  }
}