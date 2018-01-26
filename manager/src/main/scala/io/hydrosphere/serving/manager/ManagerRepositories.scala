package io.hydrosphere.serving.manager

import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.repository.db._

import scala.concurrent.ExecutionContext

trait ManagerRepositories {

  def runtimeRepository: RuntimeRepository

  def modelRepository: ModelRepository

  def modelVersionRepository: ModelVersionRepository

  def modelBuildRepository: ModelBuildRepository

  def serviceRepository: ServiceRepository

  def modelBuildScriptRepository: ModelBuildScriptRepository

  def sourceRepository: SourceConfigRepository

  def applicationRepository: ApplicationRepository

  def environmentRepository: EnvironmentRepository
}

class ManagerRepositoriesConfig(config: ManagerConfiguration)(implicit executionContext: ExecutionContext)
  extends ManagerRepositories {
  implicit val dataService = new DatabaseService(config.database)

  val runtimeRepository: RuntimeRepository = new RuntimeRepositoryImpl

  val modelRepository: ModelRepository = new ModelRepositoryImpl

  val modelVersionRepository: ModelVersionRepository = new ModelVersionRepositoryImpl

  val modelBuildRepository: ModelBuildRepository = new ModelBuildRepositoryImpl

  val serviceRepository: ServiceRepository = new ServiceRepositoryImpl

  val modelBuildScriptRepository: ModelBuildScriptRepository = new ModelBuildScriptRepositoryImpl

  val sourceRepository: SourceConfigRepository = new SourceConfigRepositoryImpl

  val applicationRepository: ApplicationRepository = new ApplicationRepositoryImpl

  val environmentRepository: EnvironmentRepository = new EnvironmentRepositoryImpl
}
