package io.hydrosphere.serving.gateway

import com.typesafe.config.Config
import io.hydrosphere.serving.config.{ApplicationConfig, Configuration, SidecarConfig}

/**
  *
  */

case class ManagerConfiguration(host: String, port: Int)

case class GatewayConfiguration(sidecar: SidecarConfig,
                                application: ApplicationConfig,
                                manager: ManagerConfiguration)

object GatewayConfiguration extends Configuration {
  def parseManager(config: Config): ManagerConfiguration = {
    val c = config.getConfig("manager")
    ManagerConfiguration(
      host = c.getString("host"),
      port = c.getInt("port")
    )
  }

  def parse(config: Config): GatewayConfiguration = GatewayConfiguration(
    sidecar = parseSidecar(config),
    application = parseApplication(config),
    manager = parseManager(config)
  )
}
