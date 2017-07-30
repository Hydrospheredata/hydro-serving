package io.hydrosphere.serving.manager

import com.spotify.docker.client.DefaultDockerClient
import io.hydrosphere.serving.manager.service.clouddriver.{DockerRuntimeDeployService, RuntimeDeployService, SwarmRuntimeDeployService}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.envoy.EnvoyManagementServiceImpl

import scala.concurrent.ExecutionContext

/**
  *
  */
class ManagerServices(
  managerRepositories: ManagerRepositories,
  managerConfiguration: ManagerConfiguration
)(implicit val ex: ExecutionContext) {

  val dockerClient = DefaultDockerClient.fromEnv().build()

  val modelManagementService: ModelManagementService = new ModelManagementServiceImpl(
    managerRepositories.runtimeTypeRepository,
    managerRepositories.modelRepository,
    managerRepositories.modelRuntimeRepository,
    managerRepositories.modelBuildRepository
  )

  val modelSources: Map[ModelSourceConfiguration, ModelSource] = managerConfiguration.modelSources
    .map(conf => conf -> ModelSource.fromConfig(conf)).toMap

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
    managerRepositories.pipelineRepository
  )

  val envoyManagementService = new EnvoyManagementServiceImpl(
    runtimeManagementService
  )
}
