package io.hydrosphere.serving.manager.repository.db

import com.typesafe.config.Config
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.hydrosphere.slick.HydrospherePostgresDriver
import org.flywaydb.core.Flyway


class DatabaseService(config:Config) {
  private val hikariConfig = new HikariConfig()
  private val maximumConnections=config.getInt("maximumPoolSize")

  hikariConfig.setJdbcUrl(config.getString("jdbcUrl"))
  hikariConfig.setUsername(config.getString("username"))
  hikariConfig.setPassword(config.getString("password"))
  hikariConfig.setDriverClassName("org.postgresql.Driver")
  hikariConfig.setMaximumPoolSize(config.getInt("maximumPoolSize"))
  hikariConfig.setInitializationFailTimeout(20000)
  val dataSource = new HikariDataSource(hikariConfig)

  //Upgrade current schema
  private[this] val flyway = new Flyway()
  flyway.setDataSource(dataSource)
  flyway.setSchemas("hydro_serving")
  flyway.migrate()

  val driver = HydrospherePostgresDriver
  import driver.api._

  val db = Database.forDataSource(dataSource, Some(maximumConnections))
}
