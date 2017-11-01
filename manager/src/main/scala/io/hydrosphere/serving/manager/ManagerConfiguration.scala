package io.hydrosphere.serving.manager

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3._
import com.amazonaws.services.sqs._
import com.typesafe.config.Config
import io.hydrosphere.serving.config._

import collection.JavaConverters._

/**
  *
  */

trait ManagerConfiguration {
  val sidecar: SidecarConfig
  val application: ApplicationConfig
  val advertised: AdvertisedConfiguration
  val modelSources: Seq[ModelSourceConfiguration]
  val database: Config
  val cloudDriver: CloudDriverConfiguration
  val zipkin: ZipkinConfiguration
  val dockerRepository: DockerRepositoryConfiguration
}

case class ManagerConfigurationImpl(
  sidecar: SidecarConfig,
  application: ApplicationConfig,
  advertised: AdvertisedConfiguration,
  modelSources: Seq[ModelSourceConfiguration],
  database: Config,
  cloudDriver: CloudDriverConfiguration,
  zipkin: ZipkinConfiguration,
  dockerRepository: DockerRepositoryConfiguration
) extends ManagerConfiguration

case class AdvertisedConfiguration(
  advertisedHost: String,
  advertisedPort: Int)

abstract class ModelSourceConfiguration() {
  val name: String
  val path: String
}

case class LocalModelSourceConfiguration(
  name: String,
  path: String
) extends ModelSourceConfiguration

case class S3ModelSourceConfiguration(
  name: String,
  path: String,
  region: Regions,
  s3Client: AmazonS3,
  sqsClient: AmazonSQS,
  bucket: String,
  queue: String
) extends ModelSourceConfiguration

abstract class DockerRepositoryConfiguration()

case class LocalDockerRepositoryConfiguration() extends DockerRepositoryConfiguration

case class ECSDockerRepositoryConfiguration(
  region: Regions,
  accountId: String
) extends DockerRepositoryConfiguration

abstract class CloudDriverConfiguration()

case class SwarmCloudDriverConfiguration(
  networkName: String
) extends CloudDriverConfiguration

case class DockerCloudDriverConfiguration(
  networkName: String,
  loggingGelfHost: Option[String]
) extends CloudDriverConfiguration

case class ECSCloudDriverConfiguration(
  region: Regions,
  cluster: String,
  accountId: String
) extends CloudDriverConfiguration

case class ZipkinConfiguration(
  host: String,
  port: Int,
  enabled: Boolean
)

object ManagerConfiguration extends Configuration {

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
        case "docker" =>
          val hasLoggingGelfHost = driverConf.hasPath("loggingGelfHost")
          DockerCloudDriverConfiguration(
            networkName = driverConf.getString("networkName"),
            loggingGelfHost = if (hasLoggingGelfHost)
              Some(driverConf.getString("loggingGelfHost")) else None
          )
        case "ecs" =>
          ECSCloudDriverConfiguration(
            region = Regions.fromName(driverConf.getString("region")),
            cluster = driverConf.getString("cluster"),
            accountId = driverConf.getString("accountId")
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

  def parseDataSources(config: Config): Seq[ModelSourceConfiguration] = {
    val c = config.getConfig("modelSources")
    //config.getAnyRef("modelSources").{ kv =>
    config.getConfig("modelSources").root().entrySet().asScala.map { kv =>
      val modelSourceConfig = c.getConfig(kv.getKey)
      val path = modelSourceConfig.getString("path")
      val name = {
        if (modelSourceConfig.hasPath("name")) {
          modelSourceConfig.getString("name")
        } else {
          kv.getKey
        }
      }

      kv.getKey match {
        case "local" =>
          LocalModelSourceConfiguration(name = name, path = path)
        case "s3" =>
          //TODO move to service
          val s3Client = AmazonS3ClientBuilder.standard().withRegion(modelSourceConfig.getString("region")).build()
          val sqsClient = AmazonSQSClientBuilder.standard().withRegion(modelSourceConfig.getString("region")).build()
          S3ModelSourceConfiguration(
            name = name,
            path = path,
            s3Client = s3Client,
            sqsClient = sqsClient,
            region = Regions.fromName(modelSourceConfig.getString("region")),
            bucket = modelSourceConfig.getString("bucket"),
            queue = modelSourceConfig.getString("queue")
          )
        case x =>
          throw new IllegalArgumentException(s"Unknown model source: $x")
      }
    }.toSeq
  }

  /*

  */

  def parse(config: Config): ManagerConfigurationImpl = ManagerConfigurationImpl(
      sidecar = parseSidecar(config),
      application = parseApplication(config),
      advertised = parseAdvertised(config),
      modelSources = parseDataSources(config),
      database = config.getConfig("database"),
      cloudDriver = parseCloudDriver(config),
      zipkin = parseZipkin(config),
      dockerRepository = parseDockerRepository(config)
    )

}