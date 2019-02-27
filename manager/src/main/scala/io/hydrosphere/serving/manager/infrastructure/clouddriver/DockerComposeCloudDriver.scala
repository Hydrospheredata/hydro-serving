package io.hydrosphere.serving.manager.infrastructure.clouddriver

import cats.effect.Async
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.{ListContainersParam, RemoveContainerParam}
import com.spotify.docker.client.messages.{Container, ContainerConfig, HostConfig, LogConfig}
import io.hydrosphere.serving.manager.config._
import io.hydrosphere.serving.manager.domain.clouddriver.DefaultConstants._
import io.hydrosphere.serving.manager.domain.clouddriver._
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage
import io.hydrosphere.serving.manager.domain.servable.Servable
import io.hydrosphere.serving.manager.infrastructure.clouddriver.docker.DockerUtil
import io.hydrosphere.serving.manager.infrastructure.envoy.events.CloudServiceDiscoveryEventBus
import io.hydrosphere.serving.manager.util.AsyncUtil
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

// TODO refactor
class DockerComposeCloudDriver[F[_]: Async](
  dockerClient: DockerClient,
  driverConfiguration: CloudDriverConfiguration.Docker,
  applicationConfig: ApplicationConfig,
  sidecarConfig: SidecarConfig,
  advertisedConfiguration: AdvertisedConfiguration,
  eventPublisher: CloudServiceDiscoveryEventBus[F]
)(
  implicit val ex: ExecutionContext
) extends CloudDriver[F] with Logging {

  override def serviceList(): F[Seq[CloudService]] = Async[F].delay {
    postProcessAllServiceList(getAllServices)
  }

  override def deployService(service: Servable, modelVersion: DockerImage, hostSelector: Option[HostSelector]): F[CloudService] = {
    AsyncUtil.futureAsync {
      Future {
        logger.debug(service)

        val javaLabels = getRuntimeLabels(service) ++ Map(
          DefaultConstants.LABEL_SERVICE_NAME -> service.serviceName,
          DefaultConstants.LABEL_SERVICE_ID -> service.id.toString
        )

        val envMap = service.configParams ++ Map(
          DefaultConstants.ENV_MODEL_DIR -> DefaultConstants.DEFAULT_MODEL_DIR.toString,
          DefaultConstants.ENV_APP_PORT -> DefaultConstants.DEFAULT_APP_PORT.toString,
          DefaultConstants.ENV_SIDECAR_HOST -> sidecarConfig.host,
          DefaultConstants.ENV_SIDECAR_PORT -> DefaultConstants.DEFAULT_SIDECAR_EGRESS_PORT,
          DefaultConstants.LABEL_SERVICE_ID -> service.id.toString
        )

        val builder = createMainApplicationHostConfigBuilder()

        val c = dockerClient.createContainer(ContainerConfig.builder()
          .image(modelVersion.fullName)
          .exposedPorts(DefaultConstants.DEFAULT_APP_PORT.toString)
          .labels(javaLabels.asJava)
          .hostConfig(builder.build())
          .env(envMap.map { case (k, v) => s"$k=$v" }.toList.asJava)
          .build(), service.serviceName)
        dockerClient.startContainer(c.id())

        val cloudService = fetchById(service.id)
        cloudService
      }
    }.flatMap { x =>
      eventPublisher.detected(x).map(_ => x)
    }
  }

  override def removeService(serviceId: Long): F[Unit] = Async[F].delay {
    if (serviceId > 0) {
      dockerClient.listContainers(
        ListContainersParam.withLabel(DefaultConstants.LABEL_SERVICE_ID, serviceId.toString),
        ListContainersParam.allContainers()
      ).asScala.foreach { s =>
        dockerClient.removeContainer(
          s.id(),
          RemoveContainerParam.forceKill(true),
          RemoveContainerParam.removeVolumes(true)
        )
      }
    }
  }

  def getAllServices: Seq[CloudService] =
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(DefaultConstants.LABEL_HS_SERVICE_MARKER, DefaultConstants.LABEL_HS_SERVICE_MARKER),
        ListContainersParam.allContainers()
      ).asScala
    )

  def startModel(service: Servable, modelVersion: DockerImage): String = {
    val javaLabels = mapAsJavaMap(getModelLabels(service))
    val config = ContainerConfig.builder()
      .image(modelVersion.fullName)
      .addVolume(DefaultConstants.DEFAULT_MODEL_DIR)
      .labels(javaLabels)
      .build()
    val container = dockerClient.createContainer(config, service.serviceName)
    container.id()
  }

  def fetchById(serviceId: Long): CloudService = {
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(DefaultConstants.LABEL_SERVICE_ID, serviceId.toString),
        ListContainersParam.allContainers()
      ).asScala
    ).headOption.getOrElse(throw new IllegalArgumentException(s"Can't find service with id=$serviceId"))
  }

  protected def postProcessAllServiceList(services: Seq[CloudService]): Seq[CloudService] = {
    services ++ DockerUtil.createFakeHttpServices(services)
  }

  protected def createMainApplicationHostConfigBuilder(): HostConfig.Builder = {
    val builder = HostConfig.builder()
      .networkMode(driverConfiguration.networkName)

    driverConfiguration.loggingConfiguration match {
      case Some(x) =>
        builder.logConfig(LogConfig.create(x.driver, x.params.asJava))
      case _ =>
    }
    builder
  }

  protected def mapMainApplicationInstance(containerApp: Container): MainApplicationInstance =
    MainApplicationInstance(
      instanceId = containerApp.id(),
      host = Option(containerApp.networkSettings().networks().get(driverConfiguration.networkName))
        .fold(containerApp.networkSettings().ipAddress())(_.ipAddress()),
      port = DefaultConstants.DEFAULT_APP_PORT
    )


  def collectCloudService(containers: Seq[Container]): Seq[CloudService] = {
    containers
      .groupBy(_.labels().get(DefaultConstants.LABEL_SERVICE_ID))
      .filter {
        case (_, v) =>
          v.exists { c =>
            val depType = c.labels().get(DefaultConstants.LABEL_DEPLOYMENT_TYPE)
            depType == DefaultConstants.DEPLOYMENT_TYPE_APP && c.state() == "running"
          }
      }
      .flatMap {
        case (k, v) =>
          try {
            Seq(
              mapToCloudService(
                k.toLong,
                v.filterNot { c =>
                  val depType = c.labels().get(DefaultConstants.LABEL_DEPLOYMENT_TYPE)
                  depType == DefaultConstants.DEPLOYMENT_TYPE_APP && c.state() != "running"
                },
                sidecarConfig
              )
            )
          } catch {
            case e: Throwable =>
              Seq.empty
          }
      }.toSeq
  }

  def mapToCloudService(serviceId: Long, seq: Seq[Container], sidecarConfig: SidecarConfig): CloudService = {
    val map = seq.map(c => c.labels().get(DefaultConstants.LABEL_DEPLOYMENT_TYPE) -> c).toMap

    val containerApp = map.getOrElse(DefaultConstants.DEPLOYMENT_TYPE_APP, throw new RuntimeException(s"Can't find APP for service $serviceId in $seq"))
    val containerModel = map.get(DefaultConstants.DEPLOYMENT_TYPE_MODEL)

    val mainApplicationInstance = mapMainApplicationInstance(containerApp)

    CloudService(
      id = serviceId,
      serviceName = Option(containerApp.labels().get(DefaultConstants.LABEL_SERVICE_NAME))
        .getOrElse(throw new RuntimeException(s"${DefaultConstants.LABEL_SERVICE_NAME} required $containerApp")),
      statusText = containerApp.status(),
      cloudDriverId = containerApp.id(),
      instances = Seq(
        ServiceInstance(
          advertisedHost = mainApplicationInstance.host,
          advertisedPort = mainApplicationInstance.port,
          instanceId = containerApp.id(),
          mainApplication = mainApplicationInstance,
          model = containerModel.map(c => {
            ModelInstance(
              c.id()
            )
          })
        )
      )
    )
  }
}