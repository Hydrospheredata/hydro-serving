package io.hydrosphere.serving.manager.service.clouddriver

import java.util

import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.DockerClient.{ListContainersParam, ListImagesParam, RemoveContainerParam}
import com.spotify.docker.client.messages.{Container, ContainerConfig, HostConfig, PortBinding}
import io.hydrosphere.serving.manager.ManagerConfiguration
import io.hydrosphere.serving.manager.model.api.ModelType
import io.hydrosphere.serving.manager.model.db.Service
import io.hydrosphere.serving.manager.service.internal_events.InternalManagerEventsPublisher
import io.hydrosphere.serving.manager.service.model_build.builders.{DockerClientHelper, ProgressHandler, ProgressMessage}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import collection.JavaConversions._
import scala.util.Try
import CloudDriverService._

/**
  *
  */
class LocalCloudDriverService(
    dockerClient: DockerClient,
    managerConfiguration: ManagerConfiguration,
    internalManagerEventsPublisher: InternalManagerEventsPublisher
)(implicit val ex: ExecutionContext) extends CloudDriverService with Logging {

  override def serviceList(): Future[Seq[CloudService]] = Future({
    postProcessAllServiceList(getAllServices())
  })

  protected def postProcessAllServiceList(services: Seq[CloudService]): Seq[CloudService] = {
    val manager=createManagerCloudService()
    val managerHttp = manager.copy(
      id = MANAGER_HTTP_ID,
      serviceName = MANAGER_HTTP_NAME,
      instances = manager.instances.map(s => s.copy(
        advertisedPort = DEFAULT_HTTP_PORT,
        mainApplication = s.mainApplication.copy(port = DEFAULT_HTTP_PORT)
      ))
    )

    services :+ managerHttp :+ manager
  }

  protected def getAllServices(): Seq[CloudService] =
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(LABEL_HS_SERVICE_MARKER, LABEL_HS_SERVICE_MARKER),
        ListContainersParam.allContainers()
      ).toSeq
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

  private def pullImage(service: Service): Unit = {
    if (dockerClient.listImages(ListImagesParam.byName(service.runtime.toImageDef)).isEmpty) {
      val handler = new ProgressHandler {
        override def handle(progressMessage: ProgressMessage): Unit = logger.info(progressMessage)
      }

      dockerClient.pull(service.runtime.toImageDef,
        DockerClientHelper.createProgressHandlerWrapper(handler))
    }
    Unit
  }

  override def deployService(service: Service): Future[CloudService] = Future.apply {
    logger.debug(service)

    pullImage(service)

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
      .labels(javaLabels)
      .hostConfig(builder.build())
      .env(envMap.map { case (k, v) => s"$k=$v" }.toList)
      .build(), s"s${service.id}app${service.serviceName}")
    dockerClient.startContainer(c.id())

    val cloudService = fetchById(service.id)
    internalManagerEventsPublisher.cloudServiceDetected(Seq(cloudService))
    cloudService
  }

  private def collectCloudService(containers: Seq[Container]): Seq[CloudService] = {
    val map = containers.groupBy(c => c.labels().get(LABEL_SERVICE_ID))
    map.entrySet().map(p => {
      Try(mapToCloudService(
        p.getKey.toLong,
        p.getValue
      ))
    }).filter(_.isSuccess).map(_.get).toSeq
  }

  protected def mapMainApplicationInstance(containerApp: Container): MainApplicationInstance =
    MainApplicationInstance(
      instanceId = containerApp.id(),
      host = Option(containerApp.networkSettings().networks().get("bridge"))
        .map(_.ipAddress()).getOrElse(managerConfiguration.sidecar.host),
      port = containerApp.ports()
        .filter(_.privatePort() == DEFAULT_APP_PORT)
        .find(_.publicPort() != null)
        .map(_.publicPort().toInt).getOrElse(DEFAULT_APP_PORT)
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

  private def fetchById(serviceId: Long): CloudService = {
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(LABEL_SERVICE_ID, serviceId.toString),
        ListContainersParam.allContainers()
      )
    ).headOption.getOrElse(throw new IllegalArgumentException(s"Can't find service with id=$serviceId"))
  }

  override def services(serviceIds: Set[Long]): Future[Seq[CloudService]] = Future.apply({
    collectCloudService(
      dockerClient.listContainers(
        ListContainersParam.withLabel(LABEL_HS_SERVICE_MARKER, LABEL_HS_SERVICE_MARKER),
        ListContainersParam.allContainers()
      ).filter(c => {
        Try(c.labels().get(LABEL_SERVICE_ID).toLong)
          .map(i => serviceIds.contains(i))
          .getOrElse(false)
      })
    )
  })

  override def removeService(serviceId: Long): Future[Unit] = Future.apply({
    if (serviceId > 0) {
      dockerClient.listContainers(
        ListContainersParam.withLabel(LABEL_SERVICE_ID, serviceId.toString),
        ListContainersParam.allContainers()
      ).foreach(s => dockerClient.removeContainer(
        s.id(),
        RemoveContainerParam.forceKill(true),
        RemoveContainerParam.removeVolumes(true)
      ))
    }
  })

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