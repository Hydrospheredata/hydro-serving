package io.hydrosphere.serving.manager

import io.hydrosphere.serving.manager.repository.{ModelRepository, RuntimeTypeRepository}
import io.hydrosphere.serving.manager.repository.db.{DatabaseService, ModelRepositoryImpl, RuntimeTypeRepositoryImpl}

import scala.concurrent.ExecutionContext

trait ManagerRepositories {
  def runtimeTypeRepository: RuntimeTypeRepository
  def modelRepository: ModelRepository

}

class ManagerRepositoriesConfig(config: ManagerConfiguration)(implicit executionContext: ExecutionContext)
  extends ManagerRepositories {
  val dataService = new DatabaseService(config.database)

  val runtimeTypeRepository: RuntimeTypeRepository = new RuntimeTypeRepositoryImpl(dataService)
  val modelRepository: ModelRepository = new ModelRepositoryImpl(dataService)
}
