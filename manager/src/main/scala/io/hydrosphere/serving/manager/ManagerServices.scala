package io.hydrosphere.serving.manager

import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.manager.service.{ModelManagementService, ModelManagementServiceImpl}

import scala.concurrent.ExecutionContext

/**
  *
  */
class ManagerServices(
  managerRepositories: ManagerRepositories,
  managerConfiguration: ManagerConfiguration
)(implicit val ex: ExecutionContext) {

  val modelManagementService: ModelManagementService = new ModelManagementServiceImpl(
    managerRepositories.runtimeTypeRepository,
    managerRepositories.modelRepository
  )

  val modelSources: Map[ModelSourceConfiguration, ModelSource] = managerConfiguration.modelSources
    .map(conf => conf -> ModelSource.fromConfig(conf)).toMap


}
