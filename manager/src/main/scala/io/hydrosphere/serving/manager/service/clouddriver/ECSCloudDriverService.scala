package io.hydrosphere.serving.manager.service.clouddriver

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.{AmazonEC2, AmazonEC2ClientBuilder}
import com.amazonaws.services.ecs.model._
import com.amazonaws.services.ecs.{AmazonECS, AmazonECSClientBuilder}
import io.hydrosphere.serving.manager.model.db.Service
import io.hydrosphere.serving.model.api.ModelType
import io.hydrosphere.serving.manager.service.internal_events._
import io.hydrosphere.serving.manager.service.actors.SelfScheduledActor
import org.apache.logging.log4j.scala.Logging

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import spray.json._
import akka.pattern.ask
import CloudDriverService._
import akka.util.Timeout
import com.amazonaws.services.route53.model.{RRType, _}
import com.amazonaws.services.route53.{AmazonRoute53, AmazonRoute53ClientBuilder}
import io.hydrosphere.serving.manager.config.{CloudDriverConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol
import io.hydrosphere.serving.manager.service.clouddriver.ECSServiceWatcherActor._

import scala.collection.mutable

class ECSCloudDriverService(
    managerConfiguration: ManagerConfiguration,
    internalManagerEventsPublisher: InternalManagerEventsPublisher
)(
    implicit val ex: ExecutionContext,
    actorSystem: ActorSystem
) extends CloudDriverService with Logging {
  implicit val timeout = Timeout(30.seconds)

  private val ecsSyncActor: ActorRef = actorSystem.actorOf(Props(classOf[ECSServiceWatcherActor], internalManagerEventsPublisher, managerConfiguration))

  override def removeService(serviceId: Long): Future[Unit] =
    (ecsSyncActor ? RemoveService(serviceId)).mapTo[Unit]

  override def deployService(service: Service): Future[CloudService] =
    (ecsSyncActor ? DeployService(service)).mapTo[CloudService]

  override def services(serviceIds: Set[Long]): Future[Seq[CloudService]] =
    (ecsSyncActor ? Services(serviceIds)).mapTo[Seq[CloudService]]

  override def getMetricServiceTargets(): Future[Seq[MetricServiceTargets]] =
    (ecsSyncActor ? GetMetricServiceTargets()).mapTo[Seq[MetricServiceTargets]]

  override def serviceList(): Future[Seq[CloudService]] =
    (ecsSyncActor ? ServiceList()).mapTo[Seq[CloudService]]
}

object ECSServiceWatcherActor {

  case class RemoveService(serviceId: Long)

  case class DeployService(service: Service)

  case class Services(serviceIds: Set[Long])

  case class GetMetricServiceTargets()

  case class ServiceList()

}

class ECSServiceWatcherActor(
    internalManagerEventsPublisher: InternalManagerEventsPublisher,
    managerConfiguration: ManagerConfiguration
) extends SelfScheduledActor(0.seconds, 30.seconds)(30.seconds) with CompleteJsonProtocol {

  val ecsCloudDriverConfiguration = managerConfiguration.cloudDriver.asInstanceOf[CloudDriverConfiguration.Ecs]

  val route53Client: AmazonRoute53 = AmazonRoute53ClientBuilder.standard()
    .build()

  val ecsClient: AmazonECS = AmazonECSClientBuilder.standard()
    .withRegion(ecsCloudDriverConfiguration.region)
    .build()

  val ecs2Client: AmazonEC2 = AmazonEC2ClientBuilder.standard()
    .withRegion(ecsCloudDriverConfiguration.region)
    .build()

  private val services = new mutable.ListBuffer[StoredService]()

  val hostedZone = getOrCreateHostedZone()
  val hostedZoneId = hostedZone.getId.substring("/hostedzone/".length)

  val managerDomainName = s"$MANAGER_NAME.${ecsCloudDriverConfiguration.internalDomainName}."

  override def recieveNonTick: Receive = {
    case RemoveService(serviceId) =>
      sender ! removeService(serviceId)

    case DeployService(service) =>
      sender ! deployService(service)

    case Services(serviceIds) =>
      sender ! services
        .filter(s => serviceIds.contains(s.cloudService.id))
        .map(_.cloudService)

    case ServiceList() =>
      sender ! services.map(_.cloudService)

    case GetMetricServiceTargets() =>
      sender ! Seq[MetricServiceTargets]()
  }

  private def createFakeServices(services: Seq[StoredService]): Seq[StoredService] = {
    services ++ services.filter(cs => fakeHttpServices.contains(cs.cloudService.id))
      .map(cs => {
        val fakeId = fakeHttpServices(cs.cloudService.id)
        val fakeName = specialNamesByIds(fakeId)

        val copyCloudService = cs.cloudService.copy(
          id = fakeId,
          serviceName = fakeName,
          instances = cs.cloudService.instances.map(i => {
            i.copy(mainApplication = i.mainApplication.copy(port = DEFAULT_HTTP_PORT))
          })
        )
        cs.copy(cloudService = copyCloudService)
      })
  }

  override def onTick(): Unit = {
    try {
      val tmp = fetchCloudServices()
      val fetchedServices = tmp ++ createFakeServices(tmp)

      val fetchedMap = fetchedServices.map(s => s.cloudService.id -> s).toMap
      val currentMap = services.map(s => s.cloudService.id -> s).toMap

      val toRemoveKeys = currentMap.keySet -- fetchedMap.keySet
      val toAddKeys = fetchedMap.keySet -- currentMap.keySet

      val toRemove = toRemoveKeys.toSeq.map(s => currentMap(s).cloudService)
      val toAdd = toAddKeys.toSeq.map(s => fetchedMap(s).cloudService)

      val equalKeys = fetchedMap.keySet -- toRemoveKeys -- toAddKeys
      val changed = equalKeys.toSeq
        .map(s => (fetchedMap(s), currentMap(s)))
        .filterNot(t => t._1.cloudService.equals(t._2.cloudService))
        .map(_._1.cloudService)

      services.clear()
      services ++= fetchedServices

      if (toRemove.nonEmpty) {
        log.debug(s"CloudService removed: $toRemove")
        internalManagerEventsPublisher.cloudServiceRemoved(toRemove)
      }
      val notifySeq = toAdd ++ changed
      if (notifySeq.nonEmpty) {
        log.debug(s"CloudService changed: $notifySeq")
        internalManagerEventsPublisher.cloudServiceDetected(notifySeq)
      }
      syncManagerDomainName(fetchedMap.values
        .filter(_.cloudService.id == MANAGER_ID)
        .map(_.cloudService)
        .toSeq
      )
    } catch {
      case e: Exception =>
        log.error(e, e.getMessage)
    }
  }

  private def getOrCreateHostedZone(): HostedZone = {
    val res = route53Client.listHostedZonesByName(new ListHostedZonesByNameRequest()
      .withDNSName(ecsCloudDriverConfiguration.internalDomainName)
    )
    if (res.getHostedZones == null || res.getHostedZones.isEmpty) {
      route53Client.createHostedZone(new CreateHostedZoneRequest()
        .withName(ecsCloudDriverConfiguration.internalDomainName)
        .withCallerReference(UUID.randomUUID().toString)
        //.withDelegationSetId()
        .withHostedZoneConfig(new HostedZoneConfig()
        .withComment("Serving DNS name")
        .withPrivateZone(true)
      ).withVPC(new VPC()
        .withVPCId(ecsCloudDriverConfiguration.vpcId)
        .withVPCRegion(VPCRegion.fromValue(ecsCloudDriverConfiguration.region.getName))
      )).getHostedZone
    } else {
      res.getHostedZones.asScala.head
    }
  }

  private def fetchAllResourceRecordSets(startRecordName: String = null, startRRType: String = null): Seq[ResourceRecordSet] = {
    val listResult = route53Client.listResourceRecordSets(new ListResourceRecordSetsRequest()
      .withStartRecordType(startRRType)
      .withStartRecordName(startRecordName)
      .withHostedZoneId(hostedZoneId)
    )

    if (listResult.getNextRecordName != null) {
      listResult.getResourceRecordSets.asScala ++ fetchAllResourceRecordSets(listResult.getNextRecordName, listResult.getNextRecordType)
    } else {
      listResult.getResourceRecordSets.asScala
    }
  }

  private def syncManagerDomainName(managers: Seq[CloudService]): Unit = {
    val ips = managers.flatMap(_.instances.map(_.mainApplication.host)).toSet

    val awsNames = fetchAllResourceRecordSets()
      .filter(p => p.getMultiValueAnswer != null && p.getMultiValueAnswer == true)
      .filter(_.getType == RRType.A.name())
      .filter(r => r.getName.equals(managerDomainName))
      .map(s => s.getResourceRecords.asScala.head.getValue -> s)
      .toMap

    val toAdd = ips -- awsNames.keySet

    val toRemove = awsNames.keySet -- ips

    val changes = toRemove.map(ip => {
      val change = new Change()
      change.setAction(ChangeAction.DELETE)
      change.setResourceRecordSet(awsNames(ip))
      change
    }) ++ toAdd.map(ip => {
      val change = new Change()
      change.setAction(ChangeAction.CREATE)
      change.setResourceRecordSet(new ResourceRecordSet()
        .withMultiValueAnswer(true)
        .withResourceRecords(new ResourceRecord().withValue(ip))
        .withName(managerDomainName)
        .withSetIdentifier(UUID.randomUUID().toString)
        .withTTL(0L)
        .withType(RRType.A)
      )
      change
    })

    if (changes.nonEmpty) {
      route53Client.changeResourceRecordSets(new ChangeResourceRecordSetsRequest()
        .withHostedZoneId(hostedZoneId)
        .withChangeBatch(new ChangeBatch()
          .withChanges(changes.asJava)
          .withComment("Update manager IPs in DNS")
        )
      )
    }
  }

  private def findAmazonServiceById(serviceId: Long): Option[com.amazonaws.services.ecs.model.Service] =
    services.find(s => s.cloudService.id == serviceId).map(_.ecsService)

  private def formatServiceName(str: String): String = {
    str.replaceAll("\\.", "-")
  }

  private def getField(jsObject: JsObject, fieldName: String): String = {
    jsObject.getFields(fieldName)
      .headOption
      .getOrElse(throw new IllegalArgumentException(s"Can't find field '$fieldName' in $jsObject"))
      .convertTo[String]
  }

  private def createService(service: Service, taskDefinition: TaskDefinition): com.amazonaws.services.ecs.model.Service = {
    val createService = new CreateServiceRequest()
      .withDesiredCount(1)
      .withCluster(ecsCloudDriverConfiguration.cluster)
      .withTaskDefinition(taskDefinition.getTaskDefinitionArn)
      .withServiceName(formatServiceName(service.serviceName))
    //TODO add autoscaling groups
    ecsClient.createService(createService).getService
  }

  private def createTaskDefinition(service: Service): TaskDefinition = {
    val portMappings = List[PortMapping](
      new PortMapping().withContainerPort(DEFAULT_APP_PORT)
    )

    val labels = getRuntimeLabels(service) ++ Map(
      LABEL_SERVICE_NAME -> service.serviceName
    )

    val envMap = service.configParams ++ Map(
      ENV_MODEL_DIR -> DEFAULT_MODEL_DIR.toString,
      ENV_APP_PORT -> DEFAULT_APP_PORT.toString,
      ENV_SIDECAR_HOST -> "ECS", //We have to implement this inside runtime container (http://169.254.169.254/latest/meta-data/local-ipv4)
      ENV_SIDECAR_PORT -> DEFAULT_SIDECAR_EGRESS_PORT,
      LABEL_SERVICE_ID -> service.id.toString
    )

    val env = envMap.map { case (k, v) => new KeyValuePair()
      .withName(k)
      .withValue(v.toString)
    }.toList

    val modelContainerDefinition = service.model.map(model => {
      val javaLabels = mapAsJavaMap(getModelLabels(service))

      new ContainerDefinition()
        .withName("model")
        .withImage(model.toImageDef)
        .withMemoryReservation(10)
        .withEssential(false)
        /*.withMountPoints(new MountPoint()
          .withContainerPath(DEFAULT_MODEL_DIR)
          .withSourceVolume("model")
        )*/
        .withDockerLabels(labels.asJava)
    })

    val containerDefinition = new ContainerDefinition()
      .withName("mainapp")
      .withImage(service.runtime.toImageDef)
      .withMemoryReservation(ecsCloudDriverConfiguration.memoryReservation)
      .withEnvironment(env.asJava)
      .withDockerLabels(labels.asJava)
      .withPortMappings(portMappings.asJava)

    modelContainerDefinition.map(cd => {
      containerDefinition.withVolumesFrom(new VolumeFrom()
        .withSourceContainer(cd.getName)
        .withReadOnly(true)
      )
    })

    ecsCloudDriverConfiguration.loggingConfiguration.map(x => {
      containerDefinition.withLogConfiguration(new LogConfiguration()
        .withLogDriver(LogDriver.fromValue(x.driver))
        .withOptions(x.params.asJava))
    })

    val registerTaskDefinition = new RegisterTaskDefinitionRequest()
      .withFamily(formatServiceName(service.serviceName))
      .withNetworkMode(NetworkMode.Bridge)
      .withContainerDefinitions(
        containerDefinition
      )

    modelContainerDefinition.map(cd => {
      registerTaskDefinition
        .withContainerDefinitions(cd)
      //.withVolumes(new Volume().withName("model"))
    })

    service.environment.map(e => {
      registerTaskDefinition.withPlacementConstraints(e.placeholders.map(p => {

        val jsObject = p.toJson.asJsObject
        new TaskDefinitionPlacementConstraint()
          .withType(getField(jsObject, "type"))
          .withExpression(getField(jsObject, "expression"))
      }).asJava)
    })

    ecsClient.registerTaskDefinition(registerTaskDefinition)
      .getTaskDefinition
  }

  def deployService(service: Service): CloudService = {
    val taskDefinition = createTaskDefinition(service)
    val ecsService = createService(service, taskDefinition)

    CloudService(
      id = service.id,
      serviceName = service.serviceName,
      statusText = ecsService.getStatus,
      cloudDriverId = ecsService.getServiceArn,
      environmentName = service.environment.map(_.name),
      runtimeInfo = MainApplicationInstanceInfo(
        runtimeId = service.runtime.id,
        runtimeName = service.runtime.name,
        runtimeVersion = service.runtime.version
      ),
      modelInfo = service.model.map(v => {
        ModelInstanceInfo(
          modelType = v.modelType,
          modelId = v.id,
          modelName = v.modelName,
          modelVersion = v.modelVersion,
          imageName = v.imageName,
          imageTag = v.imageTag
        )
      }),
      instances = Seq()
    )
  }

  /**
    *
    * @param instances EC2 instances ids
    * @return map (ec2InstanceId -> privateIp)
    */
  private def fetchAllNetworks(instances: Seq[String]): Map[String, String] = instances.sliding(9)
    .flatMap(ids => {
      val desc = ecs2Client.describeInstances(
        new DescribeInstancesRequest()
          .withInstanceIds(ids.asJava)
      )

      desc.getReservations.asScala.map(r => {
        r.getInstances.asScala.map(i => {
          i.getInstanceId -> i.getPrivateIpAddress
        })
      })
    }).flatten.toMap

  /**
    *
    * @param token
    * @return map (ec2InstanceId -> containerInstanceArn)
    */
  private def fetchClusterInstances(token: String = null): Map[String, String] = {
    val containersList = ecsClient.listContainerInstances(new ListContainerInstancesRequest()
      .withCluster(ecsCloudDriverConfiguration.cluster)
      .withNextToken(token)
    )

    val desc = ecsClient.describeContainerInstances(new DescribeContainerInstancesRequest()
      .withCluster(ecsCloudDriverConfiguration.cluster)
      .withContainerInstances(containersList.getContainerInstanceArns)
    )

    if (!desc.getFailures.isEmpty) {
      throw new RuntimeException(desc.getFailures.toString)
    }

    val result = desc.getContainerInstances.asScala.map(c => {
      c.getEc2InstanceId -> c.getContainerInstanceArn
    }).toMap

    if (containersList.getNextToken != null) {
      result ++ fetchClusterInstances(containersList.getNextToken)
    } else {
      result
    }
  }

  private def fetchTaskDefinitions(definitions: Set[String]): Seq[TaskDefinition] =
    definitions.map(id => {
      ecsClient.describeTaskDefinition(new DescribeTaskDefinitionRequest()
        .withTaskDefinition(id)
      ).getTaskDefinition
    }).toSeq


  private def fetchAllServices(token: String = null): Seq[com.amazonaws.services.ecs.model.Service] = {
    val listResult = ecsClient.listServices(new ListServicesRequest()
      .withCluster(ecsCloudDriverConfiguration.cluster)
      .withNextToken(token)
    )

    val desc = ecsClient.describeServices(new DescribeServicesRequest()
      .withCluster(ecsCloudDriverConfiguration.cluster)
      .withServices(listResult.getServiceArns)
    )

    if (!desc.getFailures.isEmpty) {
      throw new RuntimeException(desc.getFailures.toString)
    }

    if (listResult.getNextToken != null) {
      desc.getServices.asScala ++ fetchAllServices(listResult.getNextToken)
    } else {
      desc.getServices.asScala
    }
  }


  private def fetchAllTasks(token: String = null): Seq[Task] = {
    val listResult = ecsClient.listTasks(new ListTasksRequest()
      .withCluster(ecsCloudDriverConfiguration.cluster)
      .withNextToken(token)
    )

    val desc = ecsClient.describeTasks(new DescribeTasksRequest()
      .withCluster(ecsCloudDriverConfiguration.cluster)
      .withTasks(listResult.getTaskArns)
    )

    if (!desc.getFailures.isEmpty) {
      throw new RuntimeException(desc.getFailures.toString)
    }

    if (listResult.getNextToken != null) {
      desc.getTasks.asScala ++ fetchAllTasks(listResult.getNextToken)
    } else {
      desc.getTasks.asScala
    }
  }

  private def fetchFilteredAndEnrichedTasks(): Seq[EnrichedTask] = {
    val tasks = fetchAllTasks()
    if (tasks.isEmpty) {
      return Seq()
    }

    val services = fetchAllServices().map(s => s.getServiceName -> s).toMap
    if (services.isEmpty) {
      return Seq()
    }

    val taskDefinitions = fetchTaskDefinitions(services.values.map(s => {
      s.getTaskDefinition
    }).toSet).map(t => t.getTaskDefinitionArn -> t).toMap
    if (taskDefinitions.isEmpty) {
      return Seq()
    }

    val instances = fetchClusterInstances()
    if (instances.isEmpty) {
      return Seq()
    }

    val network = fetchAllNetworks(instances.keys.toSeq)
    if (network.isEmpty) {
      return Seq()
    }

    val privateIPs = instances.map(entry =>
      entry._2 -> InstanceInfo(
        ip = network.getOrElse(entry._1, ""),
        instanceId = entry._1,
        containerInstanceId = entry._2
      )
    ).filter(_._2.ip != "")


    tasks
      .filter(t => Option(t.getGroup).getOrElse("").startsWith("service:"))
      .map(t => {
        val service = services.get(t.getGroup.substring("service:".length))
        EnrichedTask(
          task = t,
          service = service,
          taskDefinition = service.flatMap(s => taskDefinitions.get(s.getTaskDefinition)),
          instanceInfo = privateIPs.get(t.getContainerInstanceArn)
        )
      })
      .filter(e => {
        e.instanceInfo.isDefined && e.service.isDefined && e.taskDefinition.isDefined
      })
      .filter(t => t.taskDefinition.get
        .getContainerDefinitions
        .asScala
        .exists(cd => {
          cd.getDockerLabels != null && cd.getDockerLabels.containsKey(LABEL_HS_SERVICE_MARKER)
        })
      )
  }

  private def mapSidecars(tasks: Seq[EnrichedTask]): Seq[SidecarInstance] = tasks
    .map(t => (t,
      t.taskDefinition.flatMap(
        _.getContainerDefinitions
          .asScala
          .find(
            _.getDockerLabels.get(CloudDriverService.LABEL_DEPLOYMENT_TYPE) == CloudDriverService.DEPLOYMENT_TYPE_SIDECAR)
      )))
    .filter(_._2.nonEmpty)
    .map(t => (t._1,
      t._2.flatMap(cd => {
        t._1.task.getContainers.asScala
          .find(c => c.getName == cd.getName)
      })
    ))
    .filter(_._2.nonEmpty)
    .map(t => {
      val info = t._1.instanceInfo.get
      val bindings = t._2.get.getNetworkBindings
        .asScala
        .map(c => c.getContainerPort.toInt -> c.getHostPort.toInt)
        .toMap

      SidecarInstance(
        instanceId = info.instanceId,
        host = info.ip,
        ingressPort = bindings.getOrElse(DEFAULT_SIDECAR_INGRESS_PORT, DEFAULT_SIDECAR_INGRESS_PORT),
        egressPort = bindings.getOrElse(DEFAULT_SIDECAR_EGRESS_PORT, DEFAULT_SIDECAR_EGRESS_PORT),
        adminPort = bindings.getOrElse(DEFAULT_SIDECAR_ADMIN_PORT, DEFAULT_SIDECAR_ADMIN_PORT)
      )
    })

  private def mapApplicationInfo(tasks: Seq[EnrichedTask]): Seq[ApplicationInfo] = tasks
    .map(t => (t,
      t.taskDefinition.flatMap(
        _.getContainerDefinitions
          .asScala
          .find(
            _.getDockerLabels.get(CloudDriverService.LABEL_DEPLOYMENT_TYPE) == CloudDriverService.DEPLOYMENT_TYPE_APP)
      ),
      t.taskDefinition.flatMap(
        _.getContainerDefinitions
          .asScala
          .find(
            _.getDockerLabels.get(CloudDriverService.LABEL_DEPLOYMENT_TYPE) == CloudDriverService.DEPLOYMENT_TYPE_MODEL)
      )
    ))
    .filter(_._2.nonEmpty)
    .map(t => ApplicationInfo(
      t._1,
      t._2,
      t._2.flatMap(cd => {
        t._1.task.getContainers
          .asScala
          .find(c => c.getName == cd.getName)
      }),
      t._3,
      t._3.flatMap(cd => {
        t._1.task.getContainers
          .asScala
          .find(c => c.getName == cd.getName)
      })
    ))
    .filter(_.appContainer.nonEmpty)

  private def fetchCloudServices(): Seq[StoredService] = {
    val tasks = fetchFilteredAndEnrichedTasks()

    val sidecars = mapSidecars(tasks).sortBy(_.host)
    val hostToSidecar = sidecars.map(s => s.host -> s).toMap

    if (sidecars.isEmpty) {
      throw new RuntimeException("Can't find sidecars")
    }

    val applicationInfos = mapApplicationInfo(tasks)
      .groupBy(_.task.service.get.getServiceArn)

    applicationInfos.map(entries => {
      val apps = entries._2
      val appInfo = apps.head
      val service = appInfo.task.service.get

      val containerDefinition = appInfo.appContainerDefinition.get
      val serviceLabels = containerDefinition.getDockerLabels
      val environment = serviceLabels.getOrDefault(CloudDriverService.LABEL_ENVIRONMENT, "")

      val imageArray = containerDefinition.getImage.split(":")
      val image = imageArray.head
      val version = if (imageArray.size > 1) imageArray.last else "latest"

      val serviceId = serviceLabels.get(LABEL_SERVICE_ID).toLong

      StoredService(CloudService(
        id = serviceId,
        serviceName = specialNamesByIds.getOrElse(serviceId, service.getServiceName),
        statusText = service.getStatus,
        cloudDriverId = service.getServiceArn,
        environmentName = if (environment == "") None else Some(environment),
        runtimeInfo = MainApplicationInstanceInfo(
          runtimeId = serviceLabels.get(LABEL_RUNTIME_ID).toLong,
          runtimeName = containerDefinition.getImage.split(":").head,
          runtimeVersion = version
        ),
        modelInfo = appInfo.modelContainerDefinition.map(cd => {
          val modelLabels = cd.getDockerLabels
          val modelImageArr = cd.getImage.split(":")
          val modelImage = modelImageArr.head
          val modelVersion = if (modelImageArr.size > 1) modelImageArr.last else "latest"

          ModelInstanceInfo(
            modelType = ModelType.fromTag(modelLabels.getOrDefault(LABEL_MODEL_TYPE, "")),
            modelId = modelLabels.get(LABEL_MODEL_VERSION_ID).toLong,
            modelName = modelLabels.get(LABEL_MODEL_NAME),
            modelVersion = modelLabels.get(LABEL_MODEL_VERSION).toLong,
            imageName = modelImage,
            imageTag = modelVersion
          )
        }),
        instances = apps.map(info => {
          val insInfo = info.task.instanceInfo.get
          val ports = info.appContainer.get
            .getNetworkBindings.asScala.map(s => s.getContainerPort.toInt -> s.getHostPort.toInt)
            .toMap

          val curSidecar = hostToSidecar.getOrElse(insInfo.ip, sidecars.head)

          ServiceInstance(
            instanceId = info.task.task.getTaskArn,
            mainApplication = MainApplicationInstance(
              instanceId = info.appContainer.get.getContainerArn,
              host = insInfo.ip,
              port = ports.getOrElse(DEFAULT_APP_PORT, DEFAULT_APP_PORT)
            ),
            sidecar = hostToSidecar.getOrElse(insInfo.ip, sidecars.head),
            model = info.modelContainer.map(m => {
              ModelInstance(
                instanceId = m.getContainerArn
              )
            }),
            advertisedHost = curSidecar.host,
            advertisedPort = curSidecar.ingressPort
          )
        })
      ), service)
    }).toSeq
  }

  def removeService(serviceId: Long): Unit =
    findAmazonServiceById(serviceId) match {
      case Some(x) =>
        ecsClient.updateService(new UpdateServiceRequest()
          .withCluster(x.getClusterArn)
          .withService(x.getServiceArn)
          .withDesiredCount(0)
          .withTaskDefinition(x.getTaskDefinition)
        )

        ecsClient.deleteService(new DeleteServiceRequest()
          .withCluster(x.getClusterArn)
          .withService(x.getServiceArn)
        )

        ecsClient.deregisterTaskDefinition(new DeregisterTaskDefinitionRequest()
          .withTaskDefinition(x.getTaskDefinition)
        )
      case None => Unit
    }

  private case class ApplicationInfo(
      task: EnrichedTask,
      appContainerDefinition: Option[ContainerDefinition],
      appContainer: Option[Container],
      modelContainerDefinition: Option[ContainerDefinition],
      modelContainer: Option[Container]
  )


  private case class InstanceInfo(
      ip: String,
      instanceId: String,
      containerInstanceId: String
  )

  private case class EnrichedTask(
      task: Task,
      service: Option[com.amazonaws.services.ecs.model.Service],
      taskDefinition: Option[TaskDefinition],
      instanceInfo: Option[InstanceInfo]
  )

  private case class StoredService(
      cloudService: CloudService,
      ecsService: com.amazonaws.services.ecs.model.Service
  )

}