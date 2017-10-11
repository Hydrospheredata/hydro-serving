package io.hydrosphere.serving.manager

import io.hydrosphere.serving.manager.repository.config.SourceConfigRepositoryImpl
import io.hydrosphere.serving.manager.repository.{RuntimeTypeBuildScriptRepository, _}
import io.hydrosphere.serving.manager.repository.db._

import scala.concurrent.ExecutionContext

trait ManagerRepositories {
  implicit val runtimeTypeRepository: RuntimeTypeRepository

  implicit val modelRepository: ModelRepository

  implicit val modelFilesRepository: ModelFilesRepository

  implicit val modelRuntimeRepository: ModelRuntimeRepository

  implicit val modelBuildRepository: ModelBuildRepository

  implicit val modelServiceRepository: ModelServiceRepository

  implicit val pipelineRepository: PipelineRepository

  implicit val endpointRepository: EndpointRepository

  implicit val runtimeTypeBuildScriptRepository: RuntimeTypeBuildScriptRepository

  implicit val sourceRepository: SourceConfigRepository

  implicit val weightedServiceRepository: WeightedServiceRepository
}

class ManagerRepositoriesConfig(implicit executionContext: ExecutionContext, config: ManagerConfiguration)
  extends ManagerRepositories {
  implicit val dataService = new DatabaseService(config.database)

  implicit val runtimeTypeRepository: RuntimeTypeRepository = new RuntimeTypeRepositoryImpl

  implicit val modelRepository: ModelRepository = new ModelRepositoryImpl

  implicit val modelFilesRepository = new ModelFilesRepositoryImpl

  implicit val modelRuntimeRepository: ModelRuntimeRepository = new ModelRuntimeRepositoryImpl

  implicit val modelBuildRepository: ModelBuildRepository = new ModelBuildRepositoryImpl

  implicit val modelServiceRepository: ModelServiceRepository = new ModelServiceRepositoryImpl

  implicit val pipelineRepository: PipelineRepository = new PipelineRepositoryImpl

  implicit val endpointRepository: EndpointRepository = new EndpointRepositoryImpl

  implicit val runtimeTypeBuildScriptRepository: RuntimeTypeBuildScriptRepository = new RuntimeTypeBuildScriptRepositoryImpl

  implicit val sourceRepository: SourceConfigRepository = new SourceConfigRepositoryImpl

  implicit val weightedServiceRepository: WeightedServiceRepository = new WeightedServiceRepositoryImpl
}
