package io.hydrosphere.serving.manager.config

import cats.syntax.either._
import com.amazonaws.regions.Regions
import io.hydrosphere.serving.manager.service.source.storages.local.LocalModelStorageDefinition
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

case class ManagerConfiguration(
  sidecar: SidecarConfig,
  application: ApplicationConfig,
  manager: AdvertisedConfiguration,
  localStorage: Option[LocalModelStorageDefinition],
  database: HikariConfiguration,
  cloudDriver: CloudDriverConfiguration,
  openTracing: OpenTracingConfiguration,
  dockerRepository: DockerRepositoryConfiguration,
  metrics: MetricsConfiguration,
  runtimePack: RuntimePackConfig
)

object ManagerConfiguration {
  implicit val regionsConfigReader = ConfigReader.fromString { str =>
    Either.catchNonFatal(Regions.fromName(str))
      .leftMap(e => CannotConvert(str, "Region", e.getMessage))
  }

  def load = pureconfig.loadConfig[ManagerConfiguration]
}

