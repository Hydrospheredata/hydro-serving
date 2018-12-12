package io.hydrosphere.serving.manager.infrastructure.clouddriver

import java.util

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.{ListContainersParam, RemoveContainerParam}
import com.spotify.docker.client.messages.{Container, ContainerConfig, HostConfig, PortBinding}
import io.hydrosphere.serving.manager.config.{CloudDriverConfiguration, LocalDockerCloudDriverServiceConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.domain.clouddriver._
import io.hydrosphere.serving.manager.domain.service.Service
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.InternalManagerEventsPublisher
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import DefaultConstants._
import io.hydrosphere.serving.manager.domain.host_selector.HostSelector
import io.hydrosphere.serving.manager.domain.image.DockerImage


class LocalCloudDriverService(
  dockerClient: DockerClient,
  managerConfiguration: ManagerConfiguration,
  internalManagerEventsPublisher: InternalManagerEventsPublisher
)(implicit val ex: ExecutionContext) extends CloudDriverAlgebra[Future] with Logging {

  override def serviceList(): Future[Seq[CloudService]] = Future({
    postProcessAllServiceList(getAllServices)
  })

  protected def postProcessAllServiceList(services: Seq[CloudService]): Seq[CloudService] = {
    val manager = createManagerCloudService()
    val managerHttp = manager.copy(
      id = DefaultConstants.MANAGER_HTTP_ID,
      serviceName = DefaultConstants.MANAGER_HTTP_NAME,
      instances = manager.instances.map(s => s.copy(
        advertisedPort = managerConfiguration.application.port,
        mainApplication = s.mainApplication.copy(port = managerConfiguration.application.port)
      ))
    )

    val localDockerCloudDriverConfiguration = managerConfiguration.cloudDriver.asInstanceOf[CloudDriverConfiguration.Local]

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
      ).asScala
    )

  private def startModel(service: Service, modelVersion: DockerImage): String = {
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
      .portBindings(createPortBindingsMap())

  private def createPortBindingsMap(): util.Map[String, util.List[PortBinding]] = {
    val publishPorts = new util.HashMap[String, util.List[PortBinding]]()
    val bindingsList = new util.ArrayList[PortBinding]()
    bindingsList.add(PortBinding.randomPort("0.0.0.0"))
    publishPorts.put(DefaultConstants.DEFAULT_APP_PORT.toString, bindingsList)
    publishPorts
  }

  override def deployService(service: Service, runtime: DockerImage, modelVersion: DockerImage, host: Option[HostSelector]): Future[CloudService] = Future.apply {
    logger.debug(service)

    startModel(service, modelVersion)

    val javaLabels = getRuntimeLabels(service) ++ Map(
      DefaultConstants.LABEL_SERVICE_NAME -> service.serviceName
    )

    val envMap = service.configParams ++ Map(
      DefaultConstants.ENV_MODEL_DIR -> DefaultConstants.DEFAULT_MODEL_DIR.toString,
      DefaultConstants.ENV_APP_PORT -> DefaultConstants.DEFAULT_APP_PORT.toString,
      DefaultConstants.ENV_SIDECAR_HOST -> managerConfiguration.sidecar.host,
      DefaultConstants.ENV_SIDECAR_PORT -> DefaultConstants.DEFAULT_SIDECAR_EGRESS_PORT,
      DefaultConstants.LABEL_SERVICE_ID -> service.id.toString
    )

    val builder = createMainApplicationHostConfigBuilder()

    builder.volumesFrom(service.serviceName)

    val c = dockerClient.createContainer(ContainerConfig.builder()
                                           .image(runtime.fullName)
                                           .exposedPorts(DefaultConstants.DEFAULT_APP_PORT.toString)
                                           .labels(javaLabels.asJava)
                                           .hostConfig(builder.build())
                                           .env(envMap.map { case (k, v) => s"$k=$v" }.toList.asJava)
                                           .build(), s"service${service.serviceName}")
    dockerClient.startContainer(c.id())

    val cloudService = fetchById(service.id)
    internalManagerEventsPublisher.cloudServiceDetected(Seq(cloudService))
    cloudService
  }

  private def collectCloudService(containers: Seq[Container]): Seq[CloudService] = {
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
                }
              )
            )
          } catch {
            case e: Throwable =>
              logger.warn(s"Error while collecting service $k: $e")
              Seq.empty
          }
      }.toSeq
  }

  protected def mapMainApplicationInstance(containerApp: Container): MainApplicationInstance =
    MainApplicationInstance(
      instanceId = containerApp.id(),
      host = Option(containerApp.networkSettings().networks().get("bridge"))
        .map(_.ipAddress())
        .getOrElse(managerConfiguration.sidecar.host),
      port = containerApp.ports().asScala
        .filter(_.privatePort() == DefaultConstants.DEFAULT_APP_PORT)
        .find(_.publicPort() != null)
        .map(_.publicPort().toInt)
        .filter(_ != 0)
        .getOrElse(DefaultConstants.DEFAULT_APP_PORT)
    )

  protected def mapToCloudService(serviceId: Long, seq: Seq[Container]): CloudService = {
    val map = seq.map(c => c.labels().get(DefaultConstants.LABEL_DEPLOYMENT_TYPE) -> c).toMap

    val containerApp = map.getOrElse(DefaultConstants.DEPLOYMENT_TYPE_APP, throw new RuntimeException(s"Can't find APP for service $serviceId in $seq"))
    val containerModel = map.get(DefaultConstants.DEPLOYMENT_TYPE_MODEL)

    val mainApplicationInstance = mapMainApplicationInstance(containerApp)

    val image = containerApp.image()
    CloudService(
      id = serviceId,
      serviceName = Option(containerApp.labels().get(DefaultConstants.LABEL_SERVICE_NAME))
        .getOrElse(throw new RuntimeException(s"${DefaultConstants.LABEL_SERVICE_NAME} required $containerApp")),
      statusText = containerApp.status(),
      cloudDriverId = containerApp.id(),
      image = DockerImage(
        name = image,
        tag = image
      ),
      instances = Seq(
        ServiceInstance(
          advertisedHost = mainApplicationInstance.host,
          advertisedPort = mainApplicationInstance.port,
          instanceId = containerApp.id(),
          mainApplication = mainApplicationInstance,
          sidecar = SidecarInstance(
            instanceId = "managerConfiguration.sidecar",
            host = managerConfiguration.sidecar.host,
            ingressPort = managerConfiguration.sidecar.ingressPort,
            egressPort = managerConfiguration.sidecar.egressPort,
            adminPort = managerConfiguration.sidecar.adminPort
          ),
          model = containerModel.map(c => {
            ModelInstance(
              c.id()
            )
          })
        )
      )
    )
  }

  private def createSystemCloudService(
    name: String,
    id: Long,
    host: String,
    port: Int,
    image: String
  ) = CloudService(
      id = id,
      serviceName = name,
      statusText = "OK",
      cloudDriverId = name,
      image = DockerImage(
        name = image,
        tag = "latest"
      ),
      instances = Seq(ServiceInstance(
        instanceId = name,
        mainApplication = MainApplicationInstance(
          instanceId = name,
          host = host,
          port = port
        ),
        sidecar = SidecarInstance(
          instanceId = "managerConfiguration.sidecar",
          host = managerConfiguration.sidecar.host,
          ingressPort = managerConfiguration.sidecar.ingressPort,
          egressPort = managerConfiguration.sidecar.egressPort,
          adminPort = managerConfiguration.sidecar.adminPort
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
      managerConfiguration.manager.advertisedHost,
      managerConfiguration.application.grpcPort,
      "hydrosphere/serving-manager"
    )

  private def createMonitoringCloudService(cfg: LocalDockerCloudDriverServiceConfiguration): CloudService =
    createSystemCloudService(
      DefaultConstants.MONITORING_NAME,
      DefaultConstants.MONITORING_ID,
      cfg.host,
      cfg.port,
      "hydrosphere/serving-sonar"
    )
  
  private def createProfilerCloudService(cfg: LocalDockerCloudDriverServiceConfiguration): CloudService =
    createSystemCloudService(
      DefaultConstants.PROFILER_NAME,
      DefaultConstants.PROFILER_ID,
      cfg.host,
      cfg.port,
      "hydrosphere/serving-data-profiler"
    )

  private def createGatewayCloudService(cfg: LocalDockerCloudDriverServiceConfiguration): CloudService =
    createSystemCloudService(
      DefaultConstants.GATEWAY_NAME,
      DefaultConstants.GATEWAY_ID,
      cfg.host,
      cfg.port,
      "hydrosphere/serving-gateway"
    )

  private def fetchById(serviceId: Long): CloudService = {
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(DefaultConstants.LABEL_SERVICE_ID, serviceId.toString),
        ListContainersParam.allContainers()
      ).asScala
    ).headOption.getOrElse(throw new IllegalArgumentException(s"Can't find service with id=$serviceId"))
  }

  override def services(serviceIds: Set[Long]): Future[Seq[CloudService]] = Future {
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(DefaultConstants.LABEL_HS_SERVICE_MARKER, DefaultConstants.LABEL_HS_SERVICE_MARKER),
        ListContainersParam.allContainers()
      ).asScala
        .filter { c =>
          Try(c.labels().get(DefaultConstants.LABEL_SERVICE_ID).toLong)
            .map(serviceIds.contains)
            .getOrElse(false)
        }
    )
  }

  override def removeService(serviceId: Long): Future[Unit] = Future {
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

  override def getMetricServiceTargets: Future[Seq[MetricServiceTargets]] =
    Future.successful(Seq(
      MetricServiceTargets(
        targets = List(s"${managerConfiguration.sidecar.host}:${managerConfiguration.sidecar.adminPort}"),

        labels = MetricServiceTargetLabels(
          job = Some("sidecar"),
          modelName = None,
          modelVersion = None,
          environment = None,
          runtimeName = Some("hydrosphere/serving-manager"),
          runtimeVersion = Some("latest"),
          serviceName = Some(DefaultConstants.MANAGER_NAME),
          serviceId = Some(DefaultConstants.MANAGER_ID.toString),
          serviceCloudDriverId = Some("managerConfiguration.sidecar"),
          serviceType = Some(DefaultConstants.DEPLOYMENT_TYPE_SIDECAR),
          instanceId = Some("managerConfiguration.sidecar")
        )
      )
    ))
}