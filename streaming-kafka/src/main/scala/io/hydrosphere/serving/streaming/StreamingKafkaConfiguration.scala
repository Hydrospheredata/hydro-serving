package io.hydrosphere.serving.streaming

import com.typesafe.config.Config
import io.hydrosphere.serving.config.{ApplicationConfig, Configuration, SidecarConfig}

case class StreamingConfig(
  sourceTopic: String,
  destinationTopic: String,
  processorRoute: String
)

case class StreamingKafkaConfiguration(
  sidecar: SidecarConfig,
  application: ApplicationConfig,
  streaming: StreamingConfig
)

object StreamingKafkaConfiguration extends Configuration {
  def parseStreaming(config: Config): StreamingConfig = {
    val c = config.getConfig("streaming")
    StreamingConfig(
      sourceTopic = c.getString("sourceTopic"),
      processorRoute = c.getString("processorRoute"),
      destinationTopic = c.getString("destinationTopic")
    )
  }

  def parse(config: Config): StreamingKafkaConfiguration = StreamingKafkaConfiguration(
    sidecar = parseSidecar(config),
    application = parseApplication(config),
    streaming = parseStreaming(config)
  )
}
