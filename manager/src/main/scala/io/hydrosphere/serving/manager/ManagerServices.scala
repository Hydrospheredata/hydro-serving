package io.hydrosphere.serving.manager

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.spotify.docker.client.DefaultDockerClient
import io.hydrosphere.serving.connector.{HttpRuntimeMeshConnector, RuntimeMeshConnector}
import io.hydrosphere.serving.manager.service.clouddriver.{DockerRuntimeDeployService, RuntimeDeployService, SwarmRuntimeDeployService}
import io.hydrosphere.serving.manager.service.modelsource.ModelSource
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.envoy.EnvoyManagementServiceImpl
import io.hydrosphere.serving.manager.service.modelbuild.{DefaultModelBuildService, ModelBuildService}

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

  val dockerClient = DefaultDockerClient.fromEnv().build()

  val runtimeMeshConnector: RuntimeMeshConnector = new HttpRuntimeMeshConnector(managerConfiguration.sidecar)

  val modelSources: Map[ModelSourceConfiguration, ModelSource] = managerConfiguration.modelSources
    .map(conf => conf -> ModelSource.fromConfig(conf)).toMap

  val modelBuildService: ModelBuildService = new DefaultModelBuildService(
    dockerClient,
    modelSources.values.toSeq
  )

  val modelManagementService: ModelManagementService = new ModelManagementServiceImpl(
    managerRepositories.runtimeTypeRepository,
    managerRepositories.modelRepository,
    managerRepositories.modelRuntimeRepository,
    managerRepositories.modelBuildRepository,
    managerRepositories.runtimeTypeBuildScriptRepository,
    modelBuildService
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
    runtimeMeshConnector
  )

  val envoyManagementService = new EnvoyManagementServiceImpl(
    runtimeManagementService
  )
}
