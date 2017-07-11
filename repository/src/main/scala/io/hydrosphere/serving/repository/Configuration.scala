package io.hydrosphere.serving.repository

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import io.hydrosphere.serving.repository.datasource.DataSource
import collection.JavaConverters._

/**
  * Created by Bulat on 31.05.2017.
  */

case class Configuration (
  address: String,
  port: Int,
  dataSources: Map[String, DataSource]
)

object Configuration {
  def apply(): Configuration = apply("config/repository.conf")
  def apply(configPath: String): Configuration = {
    val configFile = new File("config/repository.conf")
    val config = ConfigFactory.parseFile(configFile)
    Configuration(
      config.getString("repository.web.address"),
      config.getInt("repository.web.port"),
      dataSources(config)
    )
  }

  def dataSources(config: Config): Map[String, DataSource] =
    config.getConfig("repository.datasources").root().entrySet().asScala.map{ kv =>
      val value = kv.getValue.unwrapped().asInstanceOf[java.util.HashMap[String, String]].asScala.toMap
      kv.getKey -> DataSource.fromMap(value)
    }.toMap
}