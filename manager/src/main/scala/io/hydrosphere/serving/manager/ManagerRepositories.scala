package io.hydrosphere.serving.manager

import io.hydrosphere.serving.manager.repository.config.SourceConfigRepositoryImpl
import io.hydrosphere.serving.manager.repository.{RuntimeTypeBuildScriptRepository, _}
import io.hydrosphere.serving.manager.repository.db._

import scala.concurrent.ExecutionContext

trait ManagerRepositories {
  val runtimeTypeRepository: RuntimeTypeRepository

  val modelRepository: ModelRepository

  val modelFilesRepository: ModelFilesRepository

  val modelRuntimeRepository: ModelRuntimeRepository

  val modelBuildRepository: ModelBuildRepository

  val modelServiceRepository: ModelServiceRepository

  val pipelineRepository: PipelineRepository

  val endpointRepository: EndpointRepository

  val runtimeTypeBuildScriptRepository: RuntimeTypeBuildScriptRepository

  val sourceRepository: SourceConfigRepository

  val weightedServiceRepository: WeightedServiceRepository
}

class ManagerRepositoriesConfig(config: ManagerConfiguration)(implicit executionContext: ExecutionContext)
  extends ManagerRepositories {
  implicit val dataService = new DatabaseService(config.database)

  val runtimeTypeRepository: RuntimeTypeRepository = new RuntimeTypeRepositoryImpl

  val modelRepository: ModelRepository = new ModelRepositoryImpl

  val modelFilesRepository = new ModelFilesRepositoryImpl

  val modelRuntimeRepository: ModelRuntimeRepository = new ModelRuntimeRepositoryImpl

  val modelBuildRepository: ModelBuildRepository = new ModelBuildRepositoryImpl

  val modelServiceRepository: ModelServiceRepository = new ModelServiceRepositoryImpl

  val pipelineRepository: PipelineRepository = new PipelineRepositoryImpl

  val endpointRepository: EndpointRepository = new EndpointRepositoryImpl

  val runtimeTypeBuildScriptRepository: RuntimeTypeBuildScriptRepository = new RuntimeTypeBuildScriptRepositoryImpl

  val sourceRepository: SourceConfigRepository = new SourceConfigRepositoryImpl(config)

  val weightedServiceRepository: WeightedServiceRepository = new WeightedServiceRepositoryImpl
}
