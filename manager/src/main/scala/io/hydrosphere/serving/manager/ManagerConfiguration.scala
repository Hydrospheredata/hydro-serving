package io.hydrosphere.serving.manager

import java.nio.file.{Files, Paths}

import com.amazonaws.regions.Regions
import com.typesafe.config.{Config, ConfigException}
import com.zaxxer.hikari.HikariConfig
import io.hydrosphere.serving.manager.service.runtime.{CreateRuntimeRequest, DefaultRuntimes}
import io.hydrosphere.serving.manager.service.source.storages.local.LocalModelStorageDefinition

import collection.JavaConverters._

trait ManagerConfiguration {
  def sidecar: SidecarConfig

  def application: ApplicationConfig

  def advertised: AdvertisedConfiguration

  def localStorage: LocalModelStorageDefinition

  def database: HikariConfig

  def cloudDriver: CloudDriverConfiguration

  def zipkin: ZipkinConfiguration

  def dockerRepository: DockerRepositoryConfiguration

  def metrics: MetricsConfiguration

  def runtimesStarterPack: List[CreateRuntimeRequest]
}

case class ManagerConfigurationImpl(
  sidecar: SidecarConfig,
  application: ApplicationConfig,
  advertised: AdvertisedConfiguration,
  localStorage: LocalModelStorageDefinition,
  database: HikariConfig,
  cloudDriver: CloudDriverConfiguration,
  zipkin: ZipkinConfiguration,
  dockerRepository: DockerRepositoryConfiguration,
  metrics: MetricsConfiguration,
  runtimesStarterPack: List[CreateRuntimeRequest]
) extends ManagerConfiguration

case class ElasticSearchMetricsConfiguration(
    collectTimeout:Int,
    indexName: String,
    mappingName: String,
    clientUri: String
)

case class InfluxDBMetricsConfiguration(
    collectTimeout:Int,
    port:Int,
    host: String,
    dataBaseName:String
)

case class MetricsConfiguration(
    elasticSearch: Option[ElasticSearchMetricsConfiguration],
    influxDB: Option[InfluxDBMetricsConfiguration]
)

case class AdvertisedConfiguration(
    advertisedHost: String,
    advertisedPort: Int
)

case class ModelLoggingConfiguration(
    driver: String,
    params: Map[String, String]
)

abstract class DockerRepositoryConfiguration(
)

case class LocalDockerRepositoryConfiguration(
) extends DockerRepositoryConfiguration

case class ECSDockerRepositoryConfiguration(
    region: Regions,
    accountId: String
) extends DockerRepositoryConfiguration

abstract class CloudDriverConfiguration(
    loggingConfiguration: Option[ModelLoggingConfiguration]
)

case class SwarmCloudDriverConfiguration(
    networkName: String,
    loggingConfiguration: Option[ModelLoggingConfiguration]
) extends CloudDriverConfiguration(loggingConfiguration)

case class DockerCloudDriverConfiguration(
    networkName: String,
    loggingConfiguration: Option[ModelLoggingConfiguration]
) extends CloudDriverConfiguration(loggingConfiguration)


case class LocalDockerCloudDriverMonitoringConfiguration(
  host:String,
  port:Int,
  httpPort:Int
)

case class LocalDockerCloudDriverConfiguration(
    loggingConfiguration: Option[ModelLoggingConfiguration],
    monitoring:Option[LocalDockerCloudDriverMonitoringConfiguration]
) extends CloudDriverConfiguration(loggingConfiguration)

case class ECSCloudDriverConfiguration(
    region: Regions,
    cluster: String,
    accountId: String,
    loggingConfiguration: Option[ModelLoggingConfiguration],
    memoryReservation: Int = 200,
    internalDomainName: String,
    vpcId: String
) extends CloudDriverConfiguration(loggingConfiguration)

case class ZipkinConfiguration(
    host: String,
    port: Int,
    enabled: Boolean
)

case class ApplicationConfig(
    port: Int,
    grpcPort: Int,
    shadowingOn: Boolean
)

case class SidecarConfig(
    host: String,
    ingressPort: Int,
    egressPort: Int,
    adminPort: Int
)

object ManagerConfiguration {
  def parseSidecar(config: Config): SidecarConfig = {
    val c = config.getConfig("sidecar")
    SidecarConfig(
      host = c.getString("host"),
      ingressPort = c.getInt("ingressPort"),
      egressPort = c.getInt("egressPort"),
      adminPort = c.getInt("adminPort")
    )
  }

  def parseApplication(config: Config): ApplicationConfig = {
    val c = config.getConfig("application")
    ApplicationConfig(
      port = c.getInt("port"),
      grpcPort = c.getInt("grpcPort"),
      shadowingOn = c.getBoolean("shadowingOn")
    )
  }

  def parseDockerRepository(config: Config): DockerRepositoryConfiguration = {
    val repoType = config.getString("dockerRepository.type")
    repoType match {
      case "ecs" =>
        parseCloudDriver(config) match {
          case ecs: ECSCloudDriverConfiguration => ECSDockerRepositoryConfiguration(
            region = ecs.region,
            accountId = ecs.accountId
          )
          case _ => LocalDockerRepositoryConfiguration()
        }
      case _ => LocalDockerRepositoryConfiguration()
    }
  }

  def parseZipkin(config: Config): ZipkinConfiguration = {
    val c = config.getConfig("openTracing.zipkin")
    ZipkinConfiguration(
      host = c.getString("host"),
      port = c.getInt("port"),
      enabled = c.getBoolean("enabled")
    )
  }

  def parseLoggingConfiguration(config: Config): Option[ModelLoggingConfiguration] = {
    if (!config.hasPath("logging")) {
      return None
    }
    val c = config.getConfig("logging")
    if (!c.hasPath("driver")) {
      return None
    }

    Some(ModelLoggingConfiguration(
      driver = c.getString("driver"),
      params = c.root().entrySet().asScala
        .filter(_.getKey != "driver")
        .map(kv => kv.getKey -> c.getString(kv.getKey)).toMap
    ))
  }

  def parseCloudDriver(config: Config): CloudDriverConfiguration = {
    val c = config.getConfig("cloudDriver")
    //config.getAnyRef("modelSources").{ kv =>
    config.getConfig("cloudDriver").root().entrySet().asScala.map { kv =>
      val driverConf = c.getConfig(kv.getKey)

      val loggingConfiguration = parseLoggingConfiguration(driverConf)
      kv.getKey match {
        case "swarm" =>
          SwarmCloudDriverConfiguration(
            networkName = driverConf.getString("networkName"),
            loggingConfiguration = loggingConfiguration
          )
        case "docker" =>
          DockerCloudDriverConfiguration(
            networkName = driverConf.getString("networkName"),
            loggingConfiguration = loggingConfiguration
          )
        case "ecs" =>
          ECSCloudDriverConfiguration(
            region = Regions.fromName(driverConf.getString("region")),
            cluster = driverConf.getString("cluster"),
            accountId = driverConf.getString("accountId"),
            internalDomainName = driverConf.getString("internalDomainName"),
            vpcId = driverConf.getString("vpcId"),
            memoryReservation = driverConf.getInt("memoryReservation"),
            loggingConfiguration = loggingConfiguration
          )
        case _ =>
          val monitroing=if(driverConf.hasPath("monitoring")){
            val c=driverConf.getConfig("monitoring")
            Some(
              LocalDockerCloudDriverMonitoringConfiguration(
                host=c.getString("host"),
                port=c.getInt("port"),
                httpPort=c.getInt("httpPort")
              )
            )
          }else{
            None
          }
          LocalDockerCloudDriverConfiguration(
            loggingConfiguration,
            monitroing
          )
      }
    }.headOption.getOrElse(LocalDockerCloudDriverConfiguration(None, None))
  }

  def parseAdvertised(config: Config): AdvertisedConfiguration = {
    val c = config.getConfig("manager")
    AdvertisedConfiguration(
      advertisedHost = c.getString("advertisedHost"),
      advertisedPort = c.getInt("advertisedPort")
    )
  }

  def parseLocalStorage(config: Config): LocalModelStorageDefinition = {
    LocalModelStorageDefinition("localStorage", getStoragePathOrTemp(config))
  }

  def parseDatabase(config: Config): HikariConfig = {
    val database = config.getConfig("database")
    val hikariConfig = new HikariConfig()
    hikariConfig.setJdbcUrl(database.getString("jdbcUrl"))
    hikariConfig.setUsername(database.getString("username"))
    hikariConfig.setPassword(database.getString("password"))
    hikariConfig.setDriverClassName("org.postgresql.Driver")
    hikariConfig.setMaximumPoolSize(database.getInt("maximumPoolSize"))
    hikariConfig.setInitializationFailTimeout(20000)
    hikariConfig
  }

  def parseElasticSearchMetrics(config: Config): Option[ElasticSearchMetricsConfiguration] = {
    if (config.hasPath("elastic")) {
      val elasticConfig = config.getConfig("elastic")
      Some(ElasticSearchMetricsConfiguration(
        collectTimeout = elasticConfig.getInt("collectTimeout"),
        indexName = elasticConfig.getString("indexName"),
        mappingName = elasticConfig.getString("mappingName"),
        clientUri = elasticConfig.getString("clientUri")
      ))
    } else {
      None
    }
  }

  def parseInfluxDBMetrics(config: Config): Option[InfluxDBMetricsConfiguration] = {
    if (config.hasPath("influxDB")) {
      val influxConfig = config.getConfig("influxDB")
      Some(InfluxDBMetricsConfiguration(
        port = influxConfig.getInt("port"),
        host = influxConfig.getString("host"),
        collectTimeout = influxConfig.getInt("collectTimeout"),
        dataBaseName = influxConfig.getString("dataBaseName")
      ))
    } else {
      None
    }
  }

  def parseMetrics(config: Config): MetricsConfiguration = {
    val metrics = config.getConfig("metrics")
    MetricsConfiguration(
      elasticSearch = parseElasticSearchMetrics(metrics),
      influxDB = parseInfluxDBMetrics(metrics)
    )
  }

  def parseInitRuntimes(config: Config): List[CreateRuntimeRequest] = {
    val initRuntimes = config.getConfig("runtimes")
    DefaultRuntimes.getConfig(initRuntimes.getString("starterPack"))
  }

  def parse(config: Config): ManagerConfigurationImpl = ManagerConfigurationImpl(
    sidecar = parseSidecar(config),
    application = parseApplication(config),
    advertised = parseAdvertised(config),
    localStorage = parseLocalStorage(config),
    database = parseDatabase(config),
    cloudDriver = parseCloudDriver(config),
    zipkin = parseZipkin(config),
    dockerRepository = parseDockerRepository(config),
    metrics = parseMetrics(config),
    runtimesStarterPack = parseInitRuntimes(config)
  )

  def getStoragePathOrTemp(config: Config) = {
    try {
      val c = config.getConfig("localStorage")
      Paths.get(c.getString("path"))
    } catch {
      case ex: ConfigException.Missing =>
        Files.createTempDirectory("hydroservingLocalStorage")
    }
  }

}