package io.hydrosphere.serving.manager

import io.hydrosphere.serving.manager.repository.config.SourceConfigRepositoryImpl
import io.hydrosphere.serving.manager.repository.{RuntimeTypeBuildScriptRepository, _}
import io.hydrosphere.serving.manager.repository.db._

import scala.concurrent.ExecutionContext

trait ManagerRepositories {
  def runtimeTypeRepository: RuntimeTypeRepository

  def modelRepository: ModelRepository

  def modelFilesRepository: ModelFilesRepository

  def modelRuntimeRepository: ModelRuntimeRepository

  def modelBuildRepository: ModelBuildRepository

  def modelServiceRepository: ModelServiceRepository

  def pipelineRepository: PipelineRepository

  def endpointRepository: EndpointRepository

  def runtimeTypeBuildScriptRepository: RuntimeTypeBuildScriptRepository

  def sourceRepository: SourceConfigRepository
}

class ManagerRepositoriesConfig(config: ManagerConfiguration)(implicit executionContext: ExecutionContext)
  extends ManagerRepositories {
  val dataService = new DatabaseService(config.database)

  val runtimeTypeRepository: RuntimeTypeRepository = new RuntimeTypeRepositoryImpl(dataService)

  val modelRepository: ModelRepository = new ModelRepositoryImpl(dataService)

  val modelFilesRepository = new ModelFilesRepositoryImpl(dataService)

  val modelRuntimeRepository: ModelRuntimeRepository = new ModelRuntimeRepositoryImpl(dataService)

  val modelBuildRepository: ModelBuildRepository = new ModelBuildRepositoryImpl(dataService)

  val modelServiceRepository: ModelServiceRepository = new ModelServiceRepositoryImpl(dataService)

  val pipelineRepository: PipelineRepository = new PipelineRepositoryImpl(dataService)

  val endpointRepository: EndpointRepository = new EndpointRepositoryImpl(dataService)

  val runtimeTypeBuildScriptRepository: RuntimeTypeBuildScriptRepository = new RuntimeTypeBuildScriptRepositoryImpl(dataService)

  val sourceRepository: SourceConfigRepository = new SourceConfigRepositoryImpl(config.modelSources)
}
