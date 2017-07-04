package io.hydrosphere.serving.repository

import java.io.File

import com.typesafe.config.{ConfigFactory, ConfigValue}
import io.hydrosphere.serving.repository.source.ModelSource

import collection.JavaConverters._
/**
  * Created by Bulat on 31.05.2017.
  */
object Configuration {
  private val configFile = new File("config/repository.conf")
  private val config = ConfigFactory.parseFile(configFile)

  object Web {
    val address: String = config.getString("repository.web.address")
    val port: Int = config.getInt("repository.web.port")
  }

  val dataSources: Map[String, ModelSource] =
    config.getConfig("repository.datasources").root().entrySet().asScala.map{ kv =>
      val value = kv.getValue.unwrapped().asInstanceOf[java.util.HashMap[String, String]].asScala.toMap
      kv.getKey -> ModelSource.fromMap(value)
    }.toMap
}
