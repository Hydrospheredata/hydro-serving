package io.hydrosphere.serving.manager.service.clouddriver

import java.util

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.{ListContainersParam, RemoveContainerParam}
import com.spotify.docker.client.messages.{Container, ContainerConfig, HostConfig, PortBinding}
import io.hydrosphere.serving.manager.model.db.Service
import io.hydrosphere.serving.manager.service.clouddriver.CloudDriverService._
import io.hydrosphere.serving.manager.service.internal_events.InternalManagerEventsPublisher
import io.hydrosphere.serving.manager.{LocalDockerCloudDriverConfiguration, LocalDockerCloudDriverServiceConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.model.api.ModelType
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


class LocalCloudDriverService(
  dockerClient: DockerClient,
  managerConfiguration: ManagerConfiguration,
  internalManagerEventsPublisher: InternalManagerEventsPublisher
)(implicit val ex: ExecutionContext) extends CloudDriverService with Logging {

  override def serviceList(): Future[Seq[CloudService]] = Future({
    postProcessAllServiceList(getAllServices())
  })

  protected def postProcessAllServiceList(services: Seq[CloudService]): Seq[CloudService] = {
    val manager = createManagerCloudService()
    val managerHttp = manager.copy(
      id = MANAGER_HTTP_ID,
      serviceName = MANAGER_HTTP_NAME,
      instances = manager.instances.map(s => s.copy(
        advertisedPort = managerConfiguration.application.port,
        mainApplication = s.mainApplication.copy(port = managerConfiguration.application.port)
      ))
    )

    val localDockerCloudDriverConfiguration = managerConfiguration.cloudDriver.asInstanceOf[LocalDockerCloudDriverConfiguration]

    val servicesWithMonitoring = localDockerCloudDriverConfiguration.monitoring match {
      case Some(mConf) =>
        val monitoring = createMonitoringCloudService(mConf)
        val monitoringHttp = monitoring.copy(
          id = MONITORING_HTTP_ID,
          serviceName = MONITORING_HTTP_NAME,
          instances = manager.instances.map(s => s.copy(
            advertisedPort = mConf.httpPort,
            mainApplication = s.mainApplication.copy(port = mConf.httpPort)
          ))
        )
        services :+ managerHttp :+ manager :+ monitoring :+ monitoringHttp

      case None => services :+ managerHttp :+ manager
    }

    localDockerCloudDriverConfiguration.profiler match {
      case Some(profilerConf) =>
        val profiler = createProfilerCloudService(profilerConf)
        val profilerHttp = profiler.copy(
          id = PROFILER_HTTP_ID,
          serviceName = PROFILER_HTTP_NAME,
          instances = manager.instances.map(s => s.copy(
            advertisedPort = profilerConf.httpPort,
            mainApplication = s.mainApplication.copy(port = profilerConf.httpPort)
          ))
        )
        servicesWithMonitoring :+ profiler :+ profilerHttp
      case None => servicesWithMonitoring
    }
  }

  protected def getAllServices(): Seq[CloudService] =
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(LABEL_HS_SERVICE_MARKER, LABEL_HS_SERVICE_MARKER),
        ListContainersParam.allContainers()
      ).asScala.toSeq
    )

  private def startModel(service: Service): String = {
    val model = service.model.getOrElse(throw new IllegalArgumentException("ModelVersion required"))

    val javaLabels = mapAsJavaMap(getModelLabels(service))
    val container = dockerClient.createContainer(ContainerConfig.builder()
                                                   .image(model.toImageDef)
                                                   .addVolume(DEFAULT_MODEL_DIR)
                                                   .labels(javaLabels)
                                                   .build(), generateModelContainerName(service))
    container.id()
  }

  private def generateModelContainerName(service: Service): String = {
    val model = service.model.getOrElse(throw new IllegalArgumentException("ModelVersion required"))
    s"s${service.id}model${model.modelName}"
  }

  protected def createMainApplicationHostConfigBuilder(): HostConfig.Builder =
    HostConfig.builder()
      .portBindings(createPortBindingsMap())

  private def createPortBindingsMap(): util.Map[String, util.List[PortBinding]] = {
    val publishPorts = new util.HashMap[String, util.List[PortBinding]]()
    val bindingsList = new util.ArrayList[PortBinding]()
    bindingsList.add(PortBinding.randomPort("0.0.0.0"))
    publishPorts.put(DEFAULT_APP_PORT.toString, bindingsList)
    publishPorts
  }

  override def deployService(service: Service): Future[CloudService] = Future.apply {
    logger.debug(service)

    val modelContainerId = service.model.map(_ => startModel(service))
    val javaLabels = getRuntimeLabels(service) ++ Map(
      LABEL_SERVICE_NAME -> service.serviceName
    )

    val envMap = service.configParams ++ Map(
      ENV_MODEL_DIR -> DEFAULT_MODEL_DIR.toString,
      ENV_APP_PORT -> DEFAULT_APP_PORT.toString,
      ENV_SIDECAR_HOST -> managerConfiguration.sidecar.host,
      ENV_SIDECAR_PORT -> DEFAULT_SIDECAR_EGRESS_PORT,
      LABEL_SERVICE_ID -> service.id.toString
    )

    val builder = createMainApplicationHostConfigBuilder()


    modelContainerId.foreach { _ =>
      builder.volumesFrom(generateModelContainerName(service))
    }

    val c = dockerClient.createContainer(ContainerConfig.builder()
                                           .image(service.runtime.toImageDef)
                                           .exposedPorts(DEFAULT_APP_PORT.toString)
                                           .labels(javaLabels.asJava)
                                           .hostConfig(builder.build())
                                           .env(envMap.map { case (k, v) => s"$k=$v" }.toList.asJava)
                                           .build(), s"s${service.id}app${service.serviceName}")
    dockerClient.startContainer(c.id())

    val cloudService = fetchById(service.id)
    internalManagerEventsPublisher.cloudServiceDetected(Seq(cloudService))
    cloudService
  }

  private def collectCloudService(containers: Seq[Container]): Seq[CloudService] = {
    containers
      .groupBy(_.labels().get(LABEL_SERVICE_ID))
      .filter {
        case (_, v) =>
          v.exists { c =>
            val depType = c.labels().get(CloudDriverService.LABEL_DEPLOYMENT_TYPE)
            depType == CloudDriverService.DEPLOYMENT_TYPE_APP && c.state() == "running"
          }
      }
      .flatMap {
        case (k, v) =>
          try {
            Seq(
              mapToCloudService(
                k.toLong,
                v.filterNot { c =>
                  val depType = c.labels().get(CloudDriverService.LABEL_DEPLOYMENT_TYPE)
                  depType == CloudDriverService.DEPLOYMENT_TYPE_APP && c.state() != "running"
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
        .filter(_.privatePort() == DEFAULT_APP_PORT)
        .find(_.publicPort() != null)
        .map(_.publicPort().toInt)
        .filter(_ != 0)
        .getOrElse(DEFAULT_APP_PORT)
    )

  protected def mapToCloudService(serviceId: Long, seq: Seq[Container]): CloudService = {
    val map = seq.map(c => c.labels().get(CloudDriverService.LABEL_DEPLOYMENT_TYPE) -> c).toMap

    val containerApp = map.getOrElse(CloudDriverService.DEPLOYMENT_TYPE_APP, throw new RuntimeException(s"Can't find APP for service $serviceId in $seq"))
    val containerModel = map.get(CloudDriverService.DEPLOYMENT_TYPE_MODEL)

    val mainApplicationInstance = mapMainApplicationInstance(containerApp)
    CloudService(
      id = serviceId,
      serviceName = Option(containerApp.labels().get(LABEL_SERVICE_NAME))
        .getOrElse(throw new RuntimeException(s"$LABEL_SERVICE_NAME required $containerApp")),
      statusText = containerApp.status(),
      cloudDriverId = containerApp.id(),
      environmentName = None,
      runtimeInfo = MainApplicationInstanceInfo(
        runtimeId = containerApp.labels().get(LABEL_RUNTIME_ID).toLong,
        runtimeName = containerApp.image(),
        runtimeVersion = containerApp.image()
      ),
      modelInfo = containerModel.map(model => {
        ModelInstanceInfo(
          modelType = ModelType.fromTag(model.labels().get(LABEL_MODEL_TYPE)),
          modelId = model.labels().get(LABEL_MODEL_VERSION_ID).toLong,
          modelName = model.labels().get(LABEL_MODEL_NAME),
          modelVersion = model.labels().get(LABEL_MODEL_VERSION).toLong,
          imageName = model.image(),
          imageTag = model.image()
        )
      }),
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

  private def createSystemCloudService(name: String, id: Long, host: String,
    port: Int, image: String): CloudService =
    CloudService(
      id = id,
      serviceName = name,
      statusText = "OK",
      cloudDriverId = name,
      environmentName = None,
      runtimeInfo = MainApplicationInstanceInfo(
        runtimeId = id,
        runtimeName = image,
        runtimeVersion = "latest"
      ),
      modelInfo = None,
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
      CloudDriverService.MANAGER_NAME,
      CloudDriverService.MANAGER_ID,
      managerConfiguration.advertised.advertisedHost,
      managerConfiguration.application.grpcPort,
      "hydrosphere/serving-manager"
    )

  private def createMonitoringCloudService(cfg: LocalDockerCloudDriverServiceConfiguration): CloudService =
    createSystemCloudService(
      CloudDriverService.MONITORING_NAME,
      CloudDriverService.MONITORING_ID,
      cfg.host,
      cfg.port,
      "hydrosphere/serving-sonar"
    )
  
  private def createProfilerCloudService(cfg: LocalDockerCloudDriverServiceConfiguration): CloudService =
    createSystemCloudService(
      CloudDriverService.PROFILER_NAME,
      CloudDriverService.PROFILER_ID,
      cfg.host,
      cfg.port,
      "hydrosphere/serving-data-profiler"
    )

  private def fetchById(serviceId: Long): CloudService = {
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(LABEL_SERVICE_ID, serviceId.toString),
        ListContainersParam.allContainers()
      ).asScala
    ).headOption.getOrElse(throw new IllegalArgumentException(s"Can't find service with id=$serviceId"))
  }

  override def services(serviceIds: Set[Long]): Future[Seq[CloudService]] = Future {
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(LABEL_HS_SERVICE_MARKER, LABEL_HS_SERVICE_MARKER),
        ListContainersParam.allContainers()
      ).asScala
        .filter { c =>
          Try(c.labels().get(LABEL_SERVICE_ID).toLong)
            .map(serviceIds.contains)
            .getOrElse(false)
        }
    )
  }

  override def removeService(serviceId: Long): Future[Unit] = Future {
    if (serviceId > 0) {
      dockerClient.listContainers(
        ListContainersParam.withLabel(LABEL_SERVICE_ID, serviceId.toString),
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

  override def getMetricServiceTargets(): Future[Seq[MetricServiceTargets]] =
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
          serviceName = Some(CloudDriverService.MANAGER_NAME),
          serviceId = Some(CloudDriverService.MANAGER_ID.toString),
          serviceCloudDriverId = Some("managerConfiguration.sidecar"),
          serviceType = Some(CloudDriverService.DEPLOYMENT_TYPE_SIDECAR),
          instanceId = Some("managerConfiguration.sidecar")
        )
      )
    ))
}