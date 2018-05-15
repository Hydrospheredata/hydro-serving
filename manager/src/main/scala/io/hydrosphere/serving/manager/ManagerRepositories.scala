package io.hydrosphere.serving.manager

import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.repository.db._

import scala.concurrent.ExecutionContext

class ManagerRepositories(config: ManagerConfiguration)(implicit executionContext: ExecutionContext) {
  implicit val dataService = new DatabaseService(config.database)

  val runtimeRepository: RuntimeRepository = new RuntimeRepositoryImpl

  val modelRepository: ModelRepository = new ModelRepositoryImpl

  val modelVersionRepository: ModelVersionRepository = new ModelVersionRepositoryImpl

  val modelBuildRepository: ModelBuildRepository = new ModelBuildRepositoryImpl

  val serviceRepository: ServiceRepository = new ServiceRepositoryImpl

  val modelBuildScriptRepository: ModelBuildScriptRepository = new ModelBuildScriptRepositoryImpl

  val applicationRepository: ApplicationRepository = new ApplicationRepositoryImpl

  val environmentRepository: EnvironmentRepository = new EnvironmentRepositoryImpl
}
