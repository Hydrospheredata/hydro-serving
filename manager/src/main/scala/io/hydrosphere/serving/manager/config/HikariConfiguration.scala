package io.hydrosphere.serving.manager.config

case class HikariConfiguration(
  jdbcUrl: String,
  username: String,
  password: String,
  driverClassname: String = "org.postgresql.Driver",
  maximumPoolSize: Int
)
