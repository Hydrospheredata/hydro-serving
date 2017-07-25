package io.hydrosphere.serving.manager

import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.repository.db._

import scala.concurrent.ExecutionContext

trait ManagerRepositories {
  def runtimeTypeRepository: RuntimeTypeRepository

  def modelRepository: ModelRepository

  def modelRuntimeRepository: ModelRuntimeRepository

  def modelBuildRepository: ModelBuildRepository
}

class ManagerRepositoriesConfig(config: ManagerConfiguration)(implicit executionContext: ExecutionContext)
  extends ManagerRepositories {
  val dataService = new DatabaseService(config.database)

  val runtimeTypeRepository: RuntimeTypeRepository = new RuntimeTypeRepositoryImpl(dataService)

  val modelRepository: ModelRepository = new ModelRepositoryImpl(dataService)

  val modelRuntimeRepository: ModelRuntimeRepository = new ModelRuntimeRepositoryImpl(dataService)

  val modelBuildRepository: ModelBuildRepository = new ModelBuildRepositoryImpl(dataService)
}
