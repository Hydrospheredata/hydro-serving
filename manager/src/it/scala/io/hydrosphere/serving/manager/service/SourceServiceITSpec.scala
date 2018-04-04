package io.hydrosphere.serving.manager.service

import io.hydrosphere.serving.manager.ManagerConfigurationImpl
import io.hydrosphere.serving.manager.model.{LocalSourceParams, ModelSourceConfig}
import io.hydrosphere.serving.manager.test.FullIntegrationSpec

class SourceServiceITSpec extends FullIntegrationSpec {

  override def configuration: ManagerConfigurationImpl = {
    val sources = Seq(
      ModelSourceConfig[LocalSourceParams](
        1,
        "test_config",
        LocalSourceParams(None)
      ).toAux
    )
    super.configuration.copy(modelSources = sources)
  }

  "SourceService" should {
    // TODO fix
//    "add a local source" in {
//      val req = AddLocalSourceRequest(
//        "test_api", getClass.getResource("/models").getPath
//      )
//      managerServices.sourceManagementService.addLocalSource(req).map { maybeSourceConfig =>
//        assert(maybeSourceConfig.isDefined)
//        val modelSourceConfig = maybeSourceConfig.get
//        assert(modelSourceConfig.name === req.name)
//        assert(modelSourceConfig.params.isInstanceOf[LocalSourceParams], modelSourceConfig.params)
//        assert(modelSourceConfig.params.asInstanceOf[LocalSourceParams].path === req.path)
//      }
//    }
//
//    "reject similar source" in {
//      val reqSuccess = AddLocalSourceRequest(
//        "test", getClass.getResource("/models").getPath
//      )
//      val reqFail = AddLocalSourceRequest(
//        "test", getClass.getResource("/models").getPath
//      )
//      for {
//        successSource <- managerServices.sourceManagementService.addLocalSource(reqSuccess)
//        failSource <- managerServices.sourceManagementService.addLocalSource(reqFail)
//      } yield {
//        assert(successSource.isDefined)
//        assert(failSource.isEmpty)
//      }
//    }

    "list all sources (config+db)" in {
      managerServices.sourceManagementService.allSourceConfigs.map { sources =>
        println(sources)
        assert(sources.exists(_.name == "test_config"))
        assert(sources.exists(_.name == "test_api"))
        assert(sources.exists(_.name == "test"))
      }
    }
  }
}
