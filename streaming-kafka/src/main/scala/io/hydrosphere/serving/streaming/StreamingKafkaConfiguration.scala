package io.hydrosphere.serving.streaming

import com.typesafe.config.Config
import io.hydrosphere.serving.config.{ApplicationConfig, Configuration, SidecarConfig}


case class ManagerConfiguration(
  host: String,
  port: Int
)

case class StreamingConfig(
  sourceTopic: String,
  destinationTopic: String,
  processorApplication: Long
)

case class StreamingKafkaConfiguration(
  sidecar: SidecarConfig,
  application: ApplicationConfig,
  streaming: StreamingConfig,
  manager: ManagerConfiguration
)

object StreamingKafkaConfiguration extends Configuration {

  def parseManager(config: Config): ManagerConfiguration = {
    val c = config.getConfig("manager")
    ManagerConfiguration(
      host = c.getString("host"),
      port = c.getInt("port")
    )
  }

  def parseStreaming(config: Config): StreamingConfig = {
    val c = config.getConfig("streaming")
    StreamingConfig(
      sourceTopic = c.getString("sourceTopic"),
      processorApplication = c.getLong("processorApplication"),
      destinationTopic = c.getString("destinationTopic")
    )
  }

  def parse(config: Config): StreamingKafkaConfiguration = StreamingKafkaConfiguration(
    sidecar = parseSidecar(config),
    application = parseApplication(config),
    streaming = parseStreaming(config),
    manager = parseManager(config)
  )
}
