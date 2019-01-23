package io.hydrosphere.serving.manager

import com.zaxxer.hikari.HikariConfig
import io.hydrosphere.serving.manager.config.{HikariConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import io.hydrosphere.serving.manager.infrastructure.db.repository._

import scala.concurrent.ExecutionContext

class ManagerRepositories(config: ManagerConfiguration)(implicit executionContext: ExecutionContext) {
  implicit val dataService: DatabaseService = new DatabaseService(parseDatabase(config.database))

  val modelRepository = new ModelRepository

  val modelVersionRepository = new ModelVersionRepository

  val serviceRepository = new ServiceRepositoryImpl

  val applicationRepository = new ApplicationRepository

  val hostSelectorRepository = new HostSelectorRepository

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
