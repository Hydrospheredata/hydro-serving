package io.hydrosphere.serving.manager.service.clouddriver

import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.{AmazonEC2, AmazonEC2ClientBuilder}
import com.amazonaws.services.ecs.model._
import com.amazonaws.services.ecs.{AmazonECS, AmazonECSClientBuilder}
import io.hydrosphere.serving.manager.{ECSCloudDriverConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.model.{ModelService, ModelServiceInstance, ModelServiceInstanceStatus}
import org.apache.logging.log4j.scala.Logging

import collection.JavaConversions._

/**
  *
  */
class EcsRuntimeDeployService(
  ecsCloudDriverConfiguration: ECSCloudDriverConfiguration,
  managerConfiguration: ManagerConfiguration
) extends RuntimeDeployService with Logging {

  val ecsClient: AmazonECS = AmazonECSClientBuilder.standard()
    .withRegion(ecsCloudDriverConfiguration.region)
    .build()

  val ecs2Client: AmazonEC2 = AmazonEC2ClientBuilder.standard()
    .withRegion(ecsCloudDriverConfiguration.region)
    .build()


  override def deploy(runtime: ModelService): String = {
    val taskDefinition = createTaskDefinition(runtime)
    createService(runtime, taskDefinition).getServiceArn
  }

  private def createTaskDefinition(runtime: ModelService): TaskDefinition = {
    val portMappings = List[PortMapping](
      new PortMapping().withContainerPort(DEFAULT_SIDECAR_HTTP_PORT),
      new PortMapping().withContainerPort(DEFAULT_SIDECAR_ADMIN_PORT)
    )

    val env = List[KeyValuePair](
      new KeyValuePair()
        .withName(ENV_HS_SERVICE_ID)
        .withValue(runtime.serviceId.toString),
      new KeyValuePair()
        .withName(ENV_APP_HTTP_PORT)
        .withValue(DEFAULT_APP_HTTP_PORT.toString),
      new KeyValuePair()
        .withName(ENV_SIDECAR_HTTP_PORT)
        .withValue(DEFAULT_SIDECAR_HTTP_PORT.toString),
      new KeyValuePair()
        .withName(ENV_SIDECAR_ADMIN_PORT)
        .withValue(DEFAULT_SIDECAR_ADMIN_PORT.toString),
      new KeyValuePair()
        .withName(ENV_MANAGER_HOST)
        .withValue(managerConfiguration.advertised.advertisedHost),
      new KeyValuePair()
        .withName(ENV_MANAGER_PORT)
        .withValue(managerConfiguration.advertised.advertisedPort.toString),
      new KeyValuePair()
        .withName(ENV_ZIPKIN_ENABLED)
        .withValue(managerConfiguration.zipkin.enabled.toString),
      new KeyValuePair()
        .withName(ENV_ZIPKIN_HOST)
        .withValue(managerConfiguration.zipkin.host),
      new KeyValuePair()
        .withName(ENV_ZIPKIN_PORT)
        .withValue(managerConfiguration.zipkin.port.toString)
    )

    val labels = Map[String, String](
      LABEL_SERVICE_ID -> runtime.serviceId.toString,
      LABEL_HS_SERVICE_MARKER -> LABEL_HS_SERVICE_MARKER
    )


    val registerTaskDefinition = new RegisterTaskDefinitionRequest()
      .withFamily(s"${runtime.serviceName}")
      .withNetworkMode(NetworkMode.Bridge)
      .withContainerDefinitions(
        new ContainerDefinition()
          .withName(s"${runtime.serviceName}")
          .withImage(s"${runtime.modelRuntime.imageName}:${runtime.modelRuntime.imageTag}")
          .withMemoryReservation(500)
          .withEnvironment(env)
          .withDockerLabels(labels)
          .withPortMappings(portMappings)

      )

    ecsClient.registerTaskDefinition(registerTaskDefinition)
      .getTaskDefinition
  }

  private def createService(runtime: ModelService, taskDefinition: TaskDefinition): Service = {
    val createService = new CreateServiceRequest()
      .withDesiredCount(1)
      .withCluster(ecsCloudDriverConfiguration.cluster)
      .withTaskDefinition(taskDefinition.getTaskDefinitionArn)
      .withServiceName(s"${runtime.serviceName}_${runtime.serviceId}")

    ecsClient.createService(createService).getService
  }

  override def serviceList(): Seq[ServiceInfo] = {
    val listServicesResponse = ecsClient.listServices(new ListServicesRequest()
      .withCluster(ecsCloudDriverConfiguration.cluster)
    )
    val response = ecsClient.describeServices(new DescribeServicesRequest()
      .withServices(listServicesResponse.getServiceArns)
      .withCluster(ecsCloudDriverConfiguration.cluster))

    if (!response.getFailures.isEmpty) {
      throw new RuntimeException(response.getFailures.toString)
    }

    response.getServices.map(service => map(service))
  }

  private def map(service: Service): ServiceInfo = {
    val arr = service.getServiceName.split('_')
    ServiceInfo(
      id = arr.last.toLong,
      name = arr.dropRight(1).mkString("_"),
      cloudDriveId = service.getServiceArn,
      status = service.getStatus,
      statusText = service.getStatus
    )
  }

  override def service(serviceId: Long): Option[ServiceInfo] = {
    findById(serviceId).map(service => map(service))
  }

  private def findById(serviceId: Long): Option[Service] = {
    val listServicesResponse = ecsClient.listServices(new ListServicesRequest()
      .withCluster(ecsCloudDriverConfiguration.cluster)
    )
    val response = ecsClient.describeServices(new DescribeServicesRequest()
      .withCluster(ecsCloudDriverConfiguration.cluster)
      .withServices(listServicesResponse.getServiceArns)
    )

    if (!response.getFailures.isEmpty) {
      throw new RuntimeException(response.getFailures.toString)
    }

    response.getServices
      .find(s => {
        val arr = s.getServiceName.split('_')
        arr.last == serviceId.toString
      })
  }


  override def deleteService(serviceId: Long): Unit = {
    findById(serviceId) match {
      case Some(x) =>
        ecsClient.deleteService(new DeleteServiceRequest()
          .withCluster(x.getClusterArn)
          .withService(x.getServiceArn)
        )

        ecsClient.deregisterTaskDefinition(new DeregisterTaskDefinitionRequest()
          .withTaskDefinition(x.getTaskDefinition)
        )
      case None => Unit
    }
  }

  private def fetchModelServiceInstances(service: Service): Seq[ModelServiceInstance] = {
    val listTasksResult = ecsClient.listTasks(new ListTasksRequest()
      .withCluster(service.getClusterArn)
      .withServiceName(service.getServiceName)
      .withDesiredStatus(DesiredStatus.RUNNING)
    )

    if (listTasksResult.getTaskArns.nonEmpty) {
      val describeTasksResult = ecsClient.describeTasks(new DescribeTasksRequest()
        .withCluster(service.getClusterArn)
        .withTasks(listTasksResult.getTaskArns)
      )

      if (!describeTasksResult.getFailures.isEmpty) {
        throw new RuntimeException(describeTasksResult.getFailures.toString)
      }

      val mapEc2ToArn = ecsClient.describeContainerInstances(new DescribeContainerInstancesRequest()
        .withCluster(service.getClusterArn)
        .withContainerInstances(describeTasksResult.getTasks.map(task => task.getContainerInstanceArn)))
        .getContainerInstances.map(i => i.getEc2InstanceId -> i.getContainerInstanceArn).toMap

      val containerInstance = ecs2Client.describeInstances(new DescribeInstancesRequest()
        .withInstanceIds(mapEc2ToArn.keySet)
      ).getReservations.flatMap(r => r.getInstances)
        .map(i => mapEc2ToArn.getOrElse(i.getInstanceId,
          throw new RuntimeException(s"Can't find ARN for instance:${i.getInstanceId}")) -> i).toMap

      describeTasksResult.getTasks.flatMap(task => {
        task.getContainers.map(container => {
          try {
            val instance = containerInstance.getOrElse(task.getContainerInstanceArn,
              throw new RuntimeException(s"Can't find instance for ARN:${task.getContainerInstanceArn}"))

            Some(ModelServiceInstance(
              instanceId = container.getContainerArn,
              host = instance.getPrivateIpAddress,
              appPort = DEFAULT_APP_HTTP_PORT,
              sidecarPort = findHostPort(container, DEFAULT_SIDECAR_HTTP_PORT),
              sidecarAdminPort = findHostPort(container, DEFAULT_SIDECAR_ADMIN_PORT),
              serviceId = service.getServiceName.split('_').last.toLong,
              status = if ("running".equalsIgnoreCase(container.getLastStatus)) {
                ModelServiceInstanceStatus.UP
              } else {
                ModelServiceInstanceStatus.DOWN
              },
              statusText = Option(container.getReason)
            ))
          } catch {
            case ex: Throwable =>
              logger.error(s"Can't map container $container", ex)
              None
          }
        }).filter(o => o.nonEmpty).flatten
      })
    } else {
      Seq()
    }
  }

  private def findHostPort(container: Container, containerPort: Int): Int = {
    container.getNetworkBindings
      .find(n => n.getContainerPort == containerPort)
      .getOrElse(throw new RuntimeException(s"Can't find mapping for port $containerPort for ARN:${container.getContainerArn}"))
      .getHostPort
  }

  //TODO Optimize - serviceName==ServiceId
  override def serviceInstances(serviceId: Long): Seq[ModelServiceInstance] = {
    findById(serviceId) match {
      case Some(x) => fetchModelServiceInstances(x)
      case None => Seq()
    }
  }
}
