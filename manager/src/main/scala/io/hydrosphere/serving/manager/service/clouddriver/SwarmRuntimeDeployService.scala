package io.hydrosphere.serving.manager.service.clouddriver

import com.google.common.collect.ImmutableList
import com.spotify.docker.client.DockerClient
import com.spotify.docker.client.messages.swarm._
import io.hydrosphere.serving.manager._
import io.hydrosphere.serving.manager.model._
import org.apache.logging.log4j.scala.Logging

import collection.JavaConversions._

/**
  * TODO extend this class from DockerRuntimeDeployService
  */
class SwarmRuntimeDeployService(
  dockerClient: DockerClient,
  managerConfiguration: ManagerConfiguration
) extends RuntimeDeployService with Logging {

  /*override def deploy(runtime: ModelService, placeholders: Seq[Any]): String = {
    val conf = managerConfiguration.cloudDriver.asInstanceOf[SwarmCloudDriverConfiguration]

    val labels = Map[String, String](
      LABEL_SERVICE_ID -> runtime.serviceId.toString,
      LABEL_HS_SERVICE_MARKER -> LABEL_HS_SERVICE_MARKER
    )
    val javaLabels = mapAsJavaMap(labels)

    val envMap = runtime.configParams ++ Map(
      ENV_HS_SERVICE_ID -> runtime.serviceId,
      ENV_APP_HTTP_PORT -> DEFAULT_APP_HTTP_PORT,
      ENV_SIDECAR_HTTP_PORT -> DEFAULT_SIDECAR_HTTP_PORT,
      ENV_SIDECAR_ADMIN_PORT -> DEFAULT_SIDECAR_ADMIN_PORT,
      ENV_MANAGER_HOST -> managerConfiguration.advertised.advertisedHost,
      ENV_MANAGER_PORT -> managerConfiguration.advertised.advertisedPort,
      ENV_ZIPKIN_ENABLED -> managerConfiguration.zipkin.enabled,
      ENV_ZIPKIN_HOST -> managerConfiguration.zipkin.host,
      ENV_ZIPKIN_PORT -> managerConfiguration.zipkin.port
    )


    val spec: ServiceSpec = ServiceSpec.builder()
      .name(runtime.serviceName)
      .labels(javaLabels)
      .networks(NetworkAttachmentConfig.builder()
        .target(conf.networkName)
        .build()
      )
      .mode(ServiceMode.withReplicas(1))
      .endpointSpec(
        EndpointSpec.builder()
          .mode(EndpointSpec.Mode.RESOLUTION_MODE_VIP)
          .build()
      )
      .taskTemplate(
        TaskSpec.builder()
          .containerSpec(ContainerSpec.builder()
            .image(runtime.modelRuntime.toImageDef)
            .env(envMap.map { case (k, v) => s"$k=$v" }.toList)
            .labels(javaLabels)
            //.placement()
            //.networks()
            //.resources()
            //.restartPolicy(RestartPolicy.builder().)
            .build()
          ).build()
      ).build()

    dockerClient.createService(spec).id()
  }

  private def mapService(s: Service): ServiceInfo = {
    val id = s.spec().labels().get(LABEL_SERVICE_ID).toLong
    if (s.updateStatus() != null) {
      ServiceInfo(
        id = id,
        name = s.spec().name(),
        cloudDriveId = s.id(),
        status = s.updateStatus().state(),
        statusText = s.updateStatus().message(),
        configParams = mapEnvironments(s.spec().taskTemplate().containerSpec().env())
      )
    } else {
      ServiceInfo(
        id = id,
        name = s.spec().name(),
        cloudDriveId = s.id(),
        status = "",
        statusText = "",
        configParams = Map()
      )
    }
  }

  private def mapEnvironments(list: ImmutableList[String]): Map[String, String] =
    list
      .map(p => p.split("="))
      .filter(arr => arr.length > 1 && arr(0) != null && arr(1) != null)
      .map(arr => arr(0) -> arr(1)).toMap

  override def serviceList(): Seq[ServiceInfo] =
    dockerClient.listServices(
      Service.Criteria.builder
        .addLabel(LABEL_HS_SERVICE_MARKER, LABEL_HS_SERVICE_MARKER)
        .build
    ).map(s => mapService(s))

  override def deleteService(serviceId: Long): Unit =
    service(serviceId)
      .map(s => {
        dockerClient.removeService(s.cloudDriveId)
        Unit
      })

  override def serviceInstances(serviceId: Long): Seq[ModelServiceInstance] =
    serviceInstances(Task.Criteria.builder()
      .label(s"$LABEL_SERVICE_ID=$serviceId"))


  private def getInstanceHost(list: java.util.List[NetworkAttachment]): String = {
    val conf = managerConfiguration.cloudDriver.asInstanceOf[SwarmCloudDriverConfiguration]
    list.filter(n => conf.networkName == n.network.spec.name)
      .map(n => {
        val host = n.addresses().head
        if (host.contains("/")) {
          host.substring(0, host.indexOf("/"))
        } else {
          host
        }
      }).head
  }

  private def serviceInstances(criteria: Task.Criteria.Builder): Seq[ModelServiceInstance] =
    dockerClient.listTasks(criteria
      .label(LABEL_HS_SERVICE_MARKER)
      .build()).map(s => {

      val envMap = mapEnvironments(s.spec().containerSpec().env())

      ModelServiceInstance(
        instanceId = s.status().containerStatus().containerId(),
        host = getInstanceHost(s.networkAttachments),
        serviceId = s.spec().containerSpec().labels().get(LABEL_SERVICE_ID).toLong,
        status = if ("running".equalsIgnoreCase(s.status.state)) {
          ServiceInstanceStatus.UP
        } else {
          ServiceInstanceStatus.DOWN
        },
        statusText = Option(s.status.message),
        appPort = envMap.getOrDefault(ENV_APP_HTTP_PORT, DEFAULT_APP_HTTP_PORT.toString).toInt,
        sidecarPort = envMap.getOrDefault(ENV_SIDECAR_HTTP_PORT, DEFAULT_SIDECAR_HTTP_PORT.toString).toInt,
        sidecarAdminPort = envMap.getOrDefault(ENV_SIDECAR_ADMIN_PORT, DEFAULT_SIDECAR_ADMIN_PORT.toString).toInt
      )
    })

  override def service(serviceId: Long): Option[ServiceInfo] = dockerClient.listServices(
    Service.Criteria.builder
      .addLabel(LABEL_HS_SERVICE_MARKER, LABEL_HS_SERVICE_MARKER)
      .addLabel(LABEL_SERVICE_ID, serviceId.toString)
      .build
  ).headOption.map(s => mapService(s))*/
}
