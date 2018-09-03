package io.hydrosphere.serving.manager

import com.zaxxer.hikari.HikariConfig
import io.hydrosphere.serving.manager.config.{HikariConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.repository._
import io.hydrosphere.serving.manager.repository.db._

import scala.concurrent.ExecutionContext

class ManagerRepositories(config: ManagerConfiguration)(implicit executionContext: ExecutionContext) {
  implicit val dataService = new DatabaseService(parseDatabase(config.database))

  val runtimeRepository: RuntimeRepository = new RuntimeRepositoryImpl

  val runtimePullRepository: RuntimePullRepository = new RuntimePullRepositoryImpl

  val modelRepository: ModelRepository = new ModelRepositoryImpl

  val modelVersionRepository: ModelVersionRepository = new ModelVersionRepositoryImpl

  val modelBuildRepository: ModelBuildRepository = new ModelBuildRepositoryImpl

  val serviceRepository: ServiceRepository = new ServiceRepositoryImpl

  val modelBuildScriptRepository: ModelBuildScriptRepository = new ModelBuildScriptRepositoryImpl

  val applicationRepository: ApplicationRepository = new ApplicationRepositoryImpl

  val environmentRepository: EnvironmentRepository = new EnvironmentRepositoryImpl

  private def parseDatabase(hikariConfiguration: HikariConfiguration): HikariConfig = {
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(hikariConfiguration.jdbcUrl)
    hikariConfig.setUsername(hikariConfiguration.username)
    hikariConfig.setPassword(hikariConfiguration.password)
    hikariConfig.setDriverClassName(hikariConfiguration.driverClassname)
    hikariConfig.setMaximumPoolSize(hikariConfiguration.maximumPoolSize)
    hikariConfig.setInitializationFailTimeout(20000)
    hikariConfig
  }
}
