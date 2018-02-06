package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.controller.model_source.AddLocalSourceRequest
import io.hydrosphere.serving.manager.model.LocalSourceParams
import io.hydrosphere.serving.manager.test.FullIntegrationSpec

class SourceServiceITSpec extends FullIntegrationSpec {

  "SourceService" should {
    "add a local source" in {
      val req = AddLocalSourceRequest(
        "test", "/models"
      )
      managerServices.sourceManagementService.addLocalSource(req).map{ maybeSourceConfig =>
        assert(maybeSourceConfig.isDefined)
        val modelSourceConfig = maybeSourceConfig.get
        assert(modelSourceConfig.name === req.name)
        assert(modelSourceConfig.params.isInstanceOf[LocalSourceParams], modelSourceConfig.params)
        assert(modelSourceConfig.params.asInstanceOf[LocalSourceParams].path === req.path)
      }
    }
  }
}
