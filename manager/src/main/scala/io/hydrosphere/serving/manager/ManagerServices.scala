package io.hydrosphere.serving.manager

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import io.hydrosphere.serving.connector.{HttpRuntimeMeshConnector, RuntimeMeshConnector}
import io.hydrosphere.serving.manager.connector.HttpEnvoyAdminConnector
import io.hydrosphere.serving.manager.service.clouddriver.{DockerRuntimeDeployService, RuntimeDeployService, SwarmRuntimeDeployService}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.envoy.EnvoyManagementServiceImpl
import io.hydrosphere.serving.manager.service.modelbuild._
import io.hydrosphere.serving.manager.service.prometheus.PrometheusMetricsServiceImpl

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
  implicit val materializer: ActorMaterializer
) {

  val dockerClient: DockerClient = DefaultDockerClient.fromEnv().build()

  val runtimeMeshConnector: RuntimeMeshConnector = new HttpRuntimeMeshConnector(managerConfiguration.sidecar)

  val sourceManagementService = new SourceManagementServiceImpl(managerRepositories.sourceRepository)

  val modelBuildService: ModelBuildService = new LocalModelBuildService(
    dockerClient,
    sourceManagementService
  )

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
    case c: SwarmCloudDriverConfiguration => new SwarmRuntimeDeployService(dockerClient, managerConfiguration)
    case c: DockerCloudDriverConfiguration => new DockerRuntimeDeployService(dockerClient, managerConfiguration)
    //TODO change
    case c: ECSCloudDriverConfiguration => new DockerRuntimeDeployService(dockerClient, managerConfiguration)
  }

  val runtimeManagementService: RuntimeManagementService = new RuntimeManagementServiceImpl(
    runtimeDeployService,
    managerRepositories.modelServiceRepository,
    managerRepositories.modelRuntimeRepository
  )

  val servingManagementService: ServingManagementService = new ServingManagementServiceImpl(
    managerRepositories.endpointRepository,
    managerRepositories.pipelineRepository,
    managerRepositories.modelServiceRepository,
    runtimeMeshConnector,
    managerRepositories.weightedServiceRepository
  )

  val envoyManagementService = new EnvoyManagementServiceImpl(
    runtimeManagementService,
    servingManagementService
  )

  val envoyAdminConnector=new HttpEnvoyAdminConnector()

  val prometheusMetricsService = new PrometheusMetricsServiceImpl(
    runtimeManagementService,
    envoyAdminConnector
  )

  val uiManagementService = new UIManagementServiceImpl(
    managerRepositories.modelRepository,
    managerRepositories.modelRuntimeRepository,
    managerRepositories.modelBuildRepository,
    managerRepositories.modelServiceRepository,
    runtimeManagementService,
    servingManagementService,
    modelManagementService
  )
}
