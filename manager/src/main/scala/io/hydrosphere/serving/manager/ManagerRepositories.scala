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

  def runtimeTypeBuildScriptRepository: RuntimeTypeBuildScriptRepository

  def sourceRepository: SourceConfigRepository

  def applicationRepository: ApplicationRepository
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

  val runtimeTypeBuildScriptRepository: RuntimeTypeBuildScriptRepository = new RuntimeTypeBuildScriptRepositoryImpl

  val sourceRepository: SourceConfigRepository = new SourceConfigRepositoryImpl(config.modelSources)

  val applicationRepository: ApplicationRepository = new ApplicationRepositoryImpl
}
