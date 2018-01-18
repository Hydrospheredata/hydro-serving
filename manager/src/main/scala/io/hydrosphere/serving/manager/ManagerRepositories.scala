package io.hydrosphere.serving.manager

import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.repository.db._

import scala.concurrent.ExecutionContext

trait ManagerRepositories {

  val runtimeRepository: RuntimeRepository

  val modelRepository: ModelRepository

  val modelFilesRepository: ModelFilesRepository

  val modelVersionRepository: ModelVersionRepository

  val modelBuildRepository: ModelBuildRepository

  val serviceRepository: ServiceRepository

  val modelBuildScriptRepository: ModelBuildScriptRepository

  val sourceRepository: SourceConfigRepository

  val applicationRepository: ApplicationRepository

  val environmentRepository: EnvironmentRepository
}

class ManagerRepositoriesConfig(config: ManagerConfiguration)(implicit executionContext: ExecutionContext)
  extends ManagerRepositories {
  implicit val dataService = new DatabaseService(config.database)

  val runtimeRepository: RuntimeRepository = new RuntimeRepositoryImpl

  val modelRepository: ModelRepository = new ModelRepositoryImpl

  val modelFilesRepository = new ModelFilesRepositoryImpl

  val modelVersionRepository: ModelVersionRepository = new ModelVersionRepositoryImpl

  val modelBuildRepository: ModelBuildRepository = new ModelBuildRepositoryImpl

  val serviceRepository: ServiceRepository = new ServiceRepositoryImpl

  val modelBuildScriptRepository: ModelBuildScriptRepository = new ModelBuildScriptRepositoryImpl

  val sourceRepository: SourceConfigRepository = new SourceConfigRepositoryImpl

  val applicationRepository: ApplicationRepository = new ApplicationRepositoryImpl

  val environmentRepository: EnvironmentRepository = new EnvironmentRepositoryImpl
}
