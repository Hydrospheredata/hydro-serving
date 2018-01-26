package io.hydrosphere.serving.manager.configuration

import com.amazonaws.regions.Regions
import com.typesafe.config.Config
import com.zaxxer.hikari.HikariConfig
import io.hydrosphere.serving.manager.configuration.CloudDriverConfiguration._
import io.hydrosphere.serving.manager.configuration.DockerRepositoryConfiguration._
import io.hydrosphere.serving.manager.model.SourceParams.{AWSAuthKeys, LocalSourceParams, S3SourceParams}
import io.hydrosphere.serving.manager.model._

import scala.collection.JavaConverters._


case class ManagerConfigurationImpl(
  sidecar: SidecarConfig,
  application: ApplicationConfig,
  advertised: AdvertisedConfiguration,
  modelSources: Seq[ModelSourceConfigAux],
  database: HikariConfig,
  cloudDriver: CloudDriverConfiguration,
  zipkin: ZipkinConfiguration,
  dockerRepository: DockerRepositoryConfiguration
) extends ManagerConfiguration

object ManagerConfigurationImpl {
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
      grpcPort = c.getInt("grpcPort")
    )
  }

  def parseDockerRepository(config: Config): DockerRepositoryConfiguration = {
    val repoType = config.getString("dockerRepository.type")
    repoType match {
      case "local" => LocalDockerRepositoryConfiguration()
      case "ecs" =>
        parseCloudDriver(config) match {
          case ecs: ECSCloudDriverConfiguration => ECSDockerRepositoryConfiguration(
            region = ecs.region,
            accountId = ecs.accountId
          )
          case _ => throw new IllegalArgumentException(s"Specify ECS configuration in cloudDriver section")
        }
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

  def parseCloudDriver(config: Config): CloudDriverConfiguration = {
    val c = config.getConfig("cloudDriver")
    //config.getAnyRef("modelSources").{ kv =>
    config.getConfig("cloudDriver").root().entrySet().asScala.map { kv =>
      val driverConf = c.getConfig(kv.getKey)
      kv.getKey match {
        case "swarm" =>
          SwarmCloudDriverConfiguration(networkName = driverConf.getString("networkName"))
        case "localDocker" =>
          LocalDockerCloudDriverConfiguration()
        case "docker" =>
          val hasLoggingGelfHost = driverConf.hasPath("loggingGelfHost")
          DockerCloudDriverConfiguration(
            networkName = driverConf.getString("networkName"),
            loggingGelfHost = if (hasLoggingGelfHost)
              Some(driverConf.getString("loggingGelfHost")) else None
          )
        case "ecs" =>
          val hasLoggingGelfHost = driverConf.hasPath("loggingGelfHost")
          ECSCloudDriverConfiguration(
            region = Regions.fromName(driverConf.getString("region")),
            cluster = driverConf.getString("cluster"),
            accountId = driverConf.getString("accountId"),
            loggingGelfHost = if (hasLoggingGelfHost)
              Some(driverConf.getString("loggingGelfHost")) else None
          )
        case x =>
          throw new IllegalArgumentException(s"Unknown model source: $x")
      }
    }.head
  }

  def parseAdvertised(config: Config): AdvertisedConfiguration = {
    val c = config.getConfig("manager")
    AdvertisedConfiguration(
      advertisedHost = c.getString("advertisedHost"),
      advertisedPort = c.getInt("advertisedPort")
    )
  }

  def parseDataSources(config: Config): Seq[ModelSourceConfigAux] = {
    val c = config.getConfig("modelSources")
    c.root().entrySet().asScala.map { kv =>
      val modelSourceConfig = c.getConfig(kv.getKey)
      val path = modelSourceConfig.getString("path")
      val name = {
        if (modelSourceConfig.hasPath("name")) {
          modelSourceConfig.getString("name")
        } else {
          kv.getKey
        }
      }

      val params = kv.getKey match {
        case "local" =>
          LocalSourceParams(path)
        case "s3" =>
          val auth = if (modelSourceConfig.hasPath("awsAuth")) {
            val authConf = modelSourceConfig.getConfig("awsAuth")
            val keyId = authConf.getString("keyId")
            val secretKey = authConf.getString("secretKey")
            Some(AWSAuthKeys(keyId, secretKey))
          } else {
            None
          }
          S3SourceParams(
            awsAuth = auth,
            queueName = modelSourceConfig.getString("queue"),
            bucketName = modelSourceConfig.getString("bucket"),
            region = modelSourceConfig.getString("region")
          )
        case x =>
          throw new IllegalArgumentException(s"Unknown model source: $x")
      }
      ModelSourceConfig(-1, name, params).toAux
    }.toSeq
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

  def parse(config: Config): ManagerConfigurationImpl = ManagerConfigurationImpl(
    sidecar = parseSidecar(config),
    application = parseApplication(config),
    advertised = parseAdvertised(config),
    modelSources = parseDataSources(config),
    database = parseDatabase(config),
    cloudDriver = parseCloudDriver(config),
    zipkin = parseZipkin(config),
    dockerRepository = parseDockerRepository(config)
  )

}