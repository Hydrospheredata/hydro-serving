package io.hydrosphere.serving.manager

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.spotify.docker.client._
import io.hydrosphere.serving.manager.connector.{HttpEnvoyAdminConnector, HttpRuntimeMeshConnector, RuntimeMeshConnector}
import io.hydrosphere.serving.manager.service.clouddriver._
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.actors.{RepositoryReIndexActor, ServiceCacheUpdateActor}
import io.hydrosphere.serving.manager.service.envoy.EnvoyGRPCDiscoveryServiceImpl
import io.hydrosphere.serving.manager.service.modelbuild._
import io.hydrosphere.serving.manager.service.prometheus.PrometheusMetricsServiceImpl
import io.hydrosphere.serving.manager.service.ui.UIManagementServiceImpl
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

/**
  *
  */
class ManagerServices(
  managerRepositories: ManagerRepositories,
  managerConfiguration: ManagerConfiguration
)(
  implicit val ex: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer,
  implicit val timeout: Timeout
) extends Logging {

  val dockerClient: DockerClient = DefaultDockerClient.fromEnv().build()

  val runtimeMeshConnector: RuntimeMeshConnector = new HttpRuntimeMeshConnector(managerConfiguration.sidecar)

  val sourceManagementService = new SourceManagementServiceImpl(managerRepositories.sourceRepository)
  sourceManagementService.createWatchers
  managerConfiguration.modelSources.foreach(sourceManagementService.addSource)

  val modelBuildService: ModelBuildService = new LocalModelBuildService(dockerClient, sourceManagementService)

  val modelPushService: ModelPushService = managerConfiguration.dockerRepository match {
    case c: ECSDockerRepositoryConfiguration => new ECSModelPushService(dockerClient, c)
    case _ => new EmptyModelPushService
  }


  val modelManagementService: ModelManagementService = new ModelManagementServiceImpl(
    managerRepositories.runtimeTypeRepository,
    managerRepositories.modelRepository,
    managerRepositories.modelFilesRepository,
    managerRepositories.modelRuntimeRepository,
    managerRepositories.modelBuildRepository,
    managerRepositories.runtimeTypeBuildScriptRepository,
    modelBuildService,
    modelPushService
  )

  val runtimeDeployService: RuntimeDeployService = managerConfiguration.cloudDriver match {
    case _: SwarmCloudDriverConfiguration => new SwarmRuntimeDeployService(dockerClient, managerConfiguration)
    case _: DockerCloudDriverConfiguration => new DockerRuntimeDeployService(dockerClient, managerConfiguration)
    //TODO change
    case c: ECSCloudDriverConfiguration => new CachedProxyRuntimeDeployService(
      new EcsRuntimeDeployService(c, managerConfiguration)
    )
  }

  val cacheUpdateActor: Option[ActorRef] = runtimeDeployService match {
    case c: CachedProxyRuntimeDeployService =>
      Some(system.actorOf(ServiceCacheUpdateActor.props(c)))
    case _ =>
      logger.info(s"Cache disabled for RuntimeDeployService")
      None
  }

  val runtimeTypeManagementService: RuntimeTypeManagementService = new RuntimeTypeManagementServiceImpl(managerRepositories.runtimeTypeRepository)

  val runtimeManagementService: RuntimeManagementService = new RuntimeManagementServiceImpl(
    runtimeDeployService,
    managerRepositories.modelServiceRepository,
    managerRepositories.modelRuntimeRepository,
    managerRepositories.servingEnvironmentRepository
  )

  val servingManagementService: ServingManagementService = new ServingManagementServiceImpl(
    managerRepositories.modelServiceRepository,
    runtimeMeshConnector,
    managerRepositories.applicationRepository,
    runtimeManagementService
  )

  val envoyGRPCDiscoveryService = new EnvoyGRPCDiscoveryServiceImpl

  val envoyAdminConnector = new HttpEnvoyAdminConnector()

  val prometheusMetricsService = new PrometheusMetricsServiceImpl(runtimeManagementService, envoyAdminConnector)

  val uiManagementService = new UIManagementServiceImpl(
    managerRepositories.modelRepository,
    managerRepositories.modelRuntimeRepository,
    managerRepositories.modelBuildRepository,
    managerRepositories.modelServiceRepository,
    runtimeManagementService,
    servingManagementService,
    modelManagementService
  )

  val repoActor:ActorRef = system.actorOf(RepositoryReIndexActor.props(modelManagementService))
}
