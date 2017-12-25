package io.hydrosphere.serving.config

import com.typesafe.config.Config

/**
  *
  */
case class ApplicationConfig(port: Int,
                             appId: String)

case class SidecarConfig(host: String,
                         port: Int)

trait Configuration {
  def parseSidecar(config: Config): SidecarConfig = {
    val c = config.getConfig("sidecar")
    SidecarConfig(
      host = c.getString("host"),
      port = c.getInt("port")
    )
  }

  def parseApplication(config: Config): ApplicationConfig = {
    val c = config.getConfig("application")
    ApplicationConfig(
      port = c.getInt("port"),
      appId = c.getString("appId")
    )
  }
}
