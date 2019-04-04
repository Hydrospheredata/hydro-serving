package io.hydrosphere.serving.manager.config

import java.nio.file.Path

import cats.syntax.either._
import com.amazonaws.regions.Regions
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert

case class ManagerConfiguration(
  application: ApplicationConfig,
  manager: AdvertisedConfiguration,
  localStorage: Option[Path],
  database: HikariConfiguration,
  cloudDriver: CloudDriverConfiguration,
  openTracing: OpenTracingConfiguration,
  dockerRepository: DockerRepositoryConfiguration,
  metrics: MetricsConfiguration
)

object ManagerConfiguration {
  implicit val regionsConfigReader = ConfigReader.fromString { str =>
    Either.catchNonFatal(Regions.fromName(str))
      .leftMap(e => CannotConvert(str, "Region", e.getMessage))
  }

  def load = pureconfig.loadConfig[ManagerConfiguration]
}