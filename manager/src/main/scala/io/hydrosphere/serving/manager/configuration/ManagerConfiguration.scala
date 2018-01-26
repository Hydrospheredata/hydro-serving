package io.hydrosphere.serving.manager.configuration

import com.zaxxer.hikari.HikariConfig
import io.hydrosphere.serving.manager.model._


trait ManagerConfiguration {
  def sidecar: SidecarConfig
  def application: ApplicationConfig
  def advertised: AdvertisedConfiguration
  def modelSources: Seq[ModelSourceConfigAux]
  def database: HikariConfig
  def cloudDriver: CloudDriverConfiguration
  def zipkin: ZipkinConfiguration
  def dockerRepository: DockerRepositoryConfiguration
}
