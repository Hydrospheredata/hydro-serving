package io.hydrosphere.serving.manager.infrastructure.clouddriver

import cats.effect.Async
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.{ListContainersParam, RemoveContainerParam}
import com.spotify.docker.client.messages.{Container, ContainerConfig, HostConfig}
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

class LocalDockerCloudDriver[F[_]: Async](
  dockerClient: DockerClient,
  applicationConfig: ApplicationConfig,
  sidecarConfig: SidecarConfig,
  advertisedConfiguration: AdvertisedConfiguration,
  localDockerCloudDriverConfiguration: CloudDriverConfiguration.Local,
  cloudServiceBus: CloudServiceDiscoveryEventBus[F]
)(implicit val ex: ExecutionContext) extends CloudDriver[F] with Logging {

  override def serviceList(): F[Seq[CloudService]] = Async[F].delay {
    postProcessAllServiceList(getAllServices)
  }

  override def deployService(service: Servable, modelVersion: DockerImage, host: Option[HostSelector]): F[CloudService] = AsyncUtil.futureAsync {
    Future {
      logger.debug(service)

//      startModel(service, modelVersion)

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

//      builder.volumesFrom(service.serviceName)

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
    cloudServiceBus.detected(x).map(_ => x)
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

  protected def postProcessAllServiceList(services: Seq[CloudService]): Seq[CloudService] = {
    val manager = createManagerCloudService()
    val managerHttp = manager.copy(
      id = DefaultConstants.MANAGER_HTTP_ID,
      serviceName = DefaultConstants.MANAGER_HTTP_NAME,
      instances = manager.instances.map(s => s.copy(
        advertisedPort = applicationConfig.port,
        mainApplication = s.mainApplication.copy(port = applicationConfig.port)
      ))
    )

    val servicesWithMonitoring = localDockerCloudDriverConfiguration.monitoring match {
      case Some(mConf) =>
        val monitoring = createMonitoringCloudService(mConf)
        val monitoringHttp = monitoring.copy(
          id = DefaultConstants.MONITORING_HTTP_ID,
          serviceName = DefaultConstants.MONITORING_HTTP_NAME,
          instances = manager.instances.map(s => s.copy(
            advertisedPort = mConf.httpPort,
            mainApplication = s.mainApplication.copy(port = mConf.httpPort)
          ))
        )
        services :+ managerHttp :+ manager :+ monitoring :+ monitoringHttp

      case None => services :+ managerHttp :+ manager
    }

    val servicesWithProfiling = localDockerCloudDriverConfiguration.profiler match {
      case Some(profilerConf) =>
        val profiler = createProfilerCloudService(profilerConf)
        val profilerHttp = profiler.copy(
          id = DefaultConstants.PROFILER_HTTP_ID,
          serviceName = DefaultConstants.PROFILER_HTTP_NAME,
          instances = manager.instances.map(s => s.copy(
            advertisedPort = profilerConf.httpPort,
            mainApplication = s.mainApplication.copy(port = profilerConf.httpPort)
          ))
        )
        servicesWithMonitoring :+ profiler :+ profilerHttp
      case None => servicesWithMonitoring
    }

    localDockerCloudDriverConfiguration.gateway match {
      case Some(gatewayConf) =>
        val gateway = createGatewayCloudService(gatewayConf)
        val gatewayHttp = gateway.copy(
          id = DefaultConstants.GATEWAY_HTTP_ID,
          serviceName = DefaultConstants.GATEWAY_HTTP_NAME,
          instances = manager.instances.map(s => s.copy(
            advertisedPort = gatewayConf.httpPort,
            mainApplication = s.mainApplication.copy(port = gatewayConf.httpPort)
          ))
        )
        servicesWithProfiling :+ gateway :+ gatewayHttp
      case None => servicesWithProfiling
    }
  }

  protected def getAllServices: Seq[CloudService] =
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(DefaultConstants.LABEL_HS_SERVICE_MARKER, DefaultConstants.LABEL_HS_SERVICE_MARKER),
        ListContainersParam.allContainers()
      ).asScala,
      sidecarConfig
    )

  private def startModel(service: Servable, modelVersion: DockerImage): String = {
    val javaLabels = mapAsJavaMap(getModelLabels(service))
    val config = ContainerConfig.builder()
      .image(modelVersion.fullName)
      .addVolume(DefaultConstants.DEFAULT_MODEL_DIR)
      .labels(javaLabels)
      .build()
    val container = dockerClient.createContainer(config, service.serviceName)
    container.id()
  }

  protected def createMainApplicationHostConfigBuilder(): HostConfig.Builder =
    HostConfig.builder()
      .portBindings(DockerUtil.createPortBindingsMap())

  private def createSystemCloudService(
    name: String,
    id: Long,
    host: String,
    port: Int,
  ) = CloudService(
      id = id,
      serviceName = name,
      statusText = "OK",
      cloudDriverId = name,
      instances = Seq(ServiceInstance(
        instanceId = name,
        mainApplication = MainApplicationInstance(
          instanceId = name,
          host = host,
          port = port
        ),
        model = None,
        advertisedHost = host,
        advertisedPort = port
      ))
    )


  private def createManagerCloudService(): CloudService =
    createSystemCloudService(
      DefaultConstants.MANAGER_NAME,
      DefaultConstants.MANAGER_ID,
      advertisedConfiguration.advertisedHost,
      applicationConfig.grpcPort
    )

  private def createMonitoringCloudService(cfg: LocalDockerCloudDriverServiceConfiguration): CloudService =
    createSystemCloudService(
      DefaultConstants.MONITORING_NAME,
      DefaultConstants.MONITORING_ID,
      cfg.host,
      cfg.port
    )
  
  private def createProfilerCloudService(cfg: LocalDockerCloudDriverServiceConfiguration): CloudService =
    createSystemCloudService(
      DefaultConstants.PROFILER_NAME,
      DefaultConstants.PROFILER_ID,
      cfg.host,
      cfg.port
    )

  private def createGatewayCloudService(cfg: LocalDockerCloudDriverServiceConfiguration): CloudService =
    createSystemCloudService(
      DefaultConstants.GATEWAY_NAME,
      DefaultConstants.GATEWAY_ID,
      cfg.host,
      cfg.port
    )

  private def fetchById(serviceId: Long): CloudService = {
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(DefaultConstants.LABEL_SERVICE_ID, serviceId.toString),
        ListContainersParam.allContainers()
      ).asScala,
      sidecarConfig
    ).headOption.getOrElse(throw new IllegalArgumentException(s"Can't find service with id=$serviceId"))
  }


  def collectCloudService(containers: Seq[Container], sidecarConfig: SidecarConfig): Seq[CloudService] = {
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

  def mapMainApplicationInstance(containerApp: Container, defaultHost: String): MainApplicationInstance = {
    MainApplicationInstance(
      instanceId = containerApp.id(),
      host = Option(containerApp.networkSettings().networks().get("bridge")).fold(defaultHost)(_.ipAddress()),
      port = containerApp.ports().asScala
        .filter(_.privatePort() == DefaultConstants.DEFAULT_APP_PORT)
        .find(_.publicPort() != null)
        .map(_.publicPort().toInt)
        .filter(_ != 0)
        .getOrElse(DefaultConstants.DEFAULT_APP_PORT)
    )
  }

  def mapToCloudService(serviceId: Long, seq: Seq[Container], sidecarConfig: SidecarConfig): CloudService = {
    val map = seq.map(c => c.labels().get(DefaultConstants.LABEL_DEPLOYMENT_TYPE) -> c).toMap

    val containerApp = map.getOrElse(DefaultConstants.DEPLOYMENT_TYPE_APP, throw new RuntimeException(s"Can't find APP for service $serviceId in $seq"))
    val containerModel = map.get(DefaultConstants.DEPLOYMENT_TYPE_MODEL)

    val mainApplicationInstance = mapMainApplicationInstance(containerApp, sidecarConfig.host)

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