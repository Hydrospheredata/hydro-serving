package io.hydrosphere.serving.manager

import cats.effect.Async
import com.zaxxer.hikari.HikariConfig
import io.hydrosphere.serving.manager.config.{HikariConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.infrastructure.db.DatabaseService
import io.hydrosphere.serving.manager.infrastructure.db.repository._

import scala.concurrent.ExecutionContext

class ManagerRepositories[F[_]: Async](config: ManagerConfiguration)(implicit executionContext: ExecutionContext) {
  implicit val dataService: DatabaseService = new DatabaseService(parseDatabase(config.database))

  val modelRepository = new DBModelRepository[F]

  val modelVersionRepository = new DBModelVersionRepository[F]

  val servableRepository = new DBServableRepository[F]

  val applicationRepository = new DBApplicationRepository[F]

  val hostSelectorRepository = new DBHostSelectorRepository[F]

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
