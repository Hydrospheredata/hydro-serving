package io.hydrosphere.serving.config

import com.typesafe.config.Config

/**
  *
  */
case class ApplicationConfig(
  http: HttpConfig,
  grpc: GrpcConfig,
  appId: String
)

case class HttpConfig(port: Int)

case class GrpcConfig(port: Int)

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
    val appConfig = config.getConfig("application")
    val httpConfig = appConfig.getConfig("http")
    val grpcConfig = appConfig.getConfig("grpc")

    ApplicationConfig(
      appId = appConfig.getString("appId"),
      http = HttpConfig(
        port = httpConfig.getInt("port")
      ),
      grpc = GrpcConfig(
        port = grpcConfig.getInt("port")
      )
    )
  }
}
