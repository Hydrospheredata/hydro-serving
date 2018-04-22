package io.hydrosphere.serving.manager

import com.amazonaws.regions.Regions
import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.{AWSAuthKeys, LocalSourceParams, S3SourceParams}

import collection.JavaConverters._

trait ManagerConfiguration {
  def sidecar: SidecarConfig

  def application: ApplicationConfig

  def advertised: AdvertisedConfiguration

  def modelSources: Seq[ModelSourceConfig]

  def database: HikariConfig

  def cloudDriver: CloudDriverConfiguration

  def zipkin: ZipkinConfiguration

  def dockerRepository: DockerRepositoryConfiguration

  def metrics: MetricsConfiguration
}

case class ManagerConfigurationImpl(
    sidecar: SidecarConfig,
    application: ApplicationConfig,
    advertised: AdvertisedConfiguration,
    modelSources: Seq[ModelSourceConfig],
    database: HikariConfig,
    cloudDriver: CloudDriverConfiguration,
    zipkin: ZipkinConfiguration,
    dockerRepository: DockerRepositoryConfiguration,
    metrics: MetricsConfiguration
) extends ManagerConfiguration

case class ElasticSearchMetricsConfiguration(
    collectTimeout:Int,
    indexName: String,
    mappingName: String,
    clientUri: String
)

case class MetricsConfiguration(
    elasticSearch: Option[ElasticSearchMetricsConfiguration]
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

case class LocalDockerCloudDriverConfiguration(
    loggingConfiguration: Option[ModelLoggingConfiguration]
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
          LocalDockerCloudDriverConfiguration(loggingConfiguration)
      }
    }.headOption.getOrElse(LocalDockerCloudDriverConfiguration(None))
  }

  def parseAdvertised(config: Config): AdvertisedConfiguration = {
    val c = config.getConfig("manager")
    AdvertisedConfiguration(
      advertisedHost = c.getString("advertisedHost"),
      advertisedPort = c.getInt("advertisedPort")
    )
  }

  def parseDataSources(config: Config): Seq[ModelSourceConfig] = {
    val c = config.getConfig("modelSources")
    c.root().entrySet().asScala.map { kv =>
      val modelSourceConfig = c.getConfig(kv.getKey)
      val name = {
        if (modelSourceConfig.hasPath("name")) {
          modelSourceConfig.getString("name")
        } else {
          kv.getKey
        }
      }

      val params = kv.getKey match {
        case "local" =>
          val prefix = if (modelSourceConfig.hasPath("pathPrefix")) {
            Some(modelSourceConfig.getString("pathPrefix"))
          } else {
            None
          }
          LocalSourceParams(prefix)
        case "s3" =>
          S3SourceParams(
            awsAuth = parseAWSAuth(modelSourceConfig),
            path = modelSourceConfig.getString("path"),
            bucketName = modelSourceConfig.getString("bucket"),
            region = modelSourceConfig.getString("region")
          )
        case x =>
          throw new IllegalArgumentException(s"Unknown model source: $x")
      }
      ModelSourceConfig(-1, name, params)
    }.toSeq
  }

  private def parseAWSAuth(modelSourceConfig: Config) = {
    if (modelSourceConfig.hasPath("awsAuth")) {
      val authConf = modelSourceConfig.getConfig("awsAuth")
      val keyId = authConf.getString("keyId")
      val secretKey = authConf.getString("secretKey")
      Some(AWSAuthKeys(keyId, secretKey))
    } else {
      None
    }
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
      val elasticConfig=config.getConfig("elastic")
      Some(ElasticSearchMetricsConfiguration(
        collectTimeout=elasticConfig.getInt("collectTimeout"),
        indexName=elasticConfig.getString("indexName"),
        mappingName=elasticConfig.getString("mappingName"),
        clientUri=elasticConfig.getString("clientUri")
      ))
    } else {
      None
    }
  }

  def parseMetrics(config: Config): MetricsConfiguration = {
    val metrics = config.getConfig("metrics")
    MetricsConfiguration(
      elasticSearch = parseElasticSearchMetrics(metrics)
    )
  }

  def parse(config: Config): ManagerConfigurationImpl = ManagerConfigurationImpl(
    sidecar = parseSidecar(config),
    application = parseApplication(config),
    advertised = parseAdvertised(config),
    modelSources = parseDataSources(config),
    database = parseDatabase(config),
    cloudDriver = parseCloudDriver(config),
    zipkin = parseZipkin(config),
    dockerRepository = parseDockerRepository(config),
    metrics = parseMetrics(config)
  )

}