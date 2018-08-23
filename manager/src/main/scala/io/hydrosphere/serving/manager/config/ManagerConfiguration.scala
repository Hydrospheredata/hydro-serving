package io.hydrosphere.serving.manager.config

import com.amazonaws.regions.Regions
import io.hydrosphere.serving.manager.service.source.storages.local.LocalModelStorageDefinition
import pureconfig.ConfigReader

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
  implicit val regionsConfigReader = ConfigReader.fromCursor { cur =>
    for {
      obj <- cur.asObjectCursor
      regionC <- obj.atKey("region")
      region <- ConfigReader[String].from(regionC)
    } yield Regions.fromName(region)
  }

  def load = pureconfig.loadConfig[ManagerConfiguration]
}

