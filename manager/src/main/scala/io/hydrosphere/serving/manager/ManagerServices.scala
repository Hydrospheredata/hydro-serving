package io.hydrosphere.serving.manager

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.spotify.docker.client.{DefaultDockerClient, DockerClient}
import io.hydrosphere.serving.connector.{HttpRuntimeMeshConnector, RuntimeMeshConnector}
import io.hydrosphere.serving.manager.service.clouddriver.{DockerRuntimeDeployService, RuntimeDeployService, SwarmRuntimeDeployService}
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.envoy.EnvoyManagementServiceImpl
import io.hydrosphere.serving.manager.service.modelbuild.{EmptyModelPushService, LocalModelBuildService, ModelBuildService, ModelPushService}

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

  val modelPushService: ModelPushService = new EmptyModelPushService

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
