package io.hydrosphere.serving.manager.repository.db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.hydrosphere.slick.HydrospherePostgresDriver
import org.flywaydb.core.Flyway


class DatabaseService(hikariConfig: HikariConfig) {
  val dataSource = new HikariDataSource(hikariConfig)

  //Upgrade current schema
  private[this] val flyway = new Flyway()
  flyway.setDataSource(dataSource)
  flyway.setSchemas("hydro_serving")
  flyway.migrate()

  val driver = HydrospherePostgresDriver
  import driver.api._

  val db = Database.forDataSource(dataSource, Some(hikariConfig.getMaximumPoolSize))
}
