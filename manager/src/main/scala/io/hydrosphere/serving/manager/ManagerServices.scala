package io.hydrosphere.serving.manager

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.spotify.docker.client._
import io.hydrosphere.serving.connector._
import io.hydrosphere.serving.manager.connector.HttpEnvoyAdminConnector
import io.hydrosphere.serving.manager.service.clouddriver._
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.envoy.EnvoyManagementServiceImpl
import io.hydrosphere.serving.manager.service.modelbuild._
import io.hydrosphere.serving.manager.service.prometheus.PrometheusMetricsServiceImpl

import scala.concurrent.ExecutionContext

/**
  *
  */
class ManagerServices(
  implicit val ex: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer,
  managerRepositories: ManagerRepositories,
  managerConfiguration: ManagerConfiguration
) {

  import managerRepositories._
  import managerConfiguration._

  implicit val dockerClient: DockerClient = DefaultDockerClient.fromEnv().build()

  implicit val runtimeMeshConnector: RuntimeMeshConnector = new HttpRuntimeMeshConnector

  implicit val sourceManagementService = new SourceManagementServiceImpl

  implicit val modelBuildService: ModelBuildService = new LocalModelBuildService

  implicit val modelPushService: ModelPushService = managerConfiguration.dockerRepository match {
    case c: ECSDockerRepositoryConfiguration => new ECSModelPushService(dockerClient, c)
    case _ => new EmptyModelPushService
  }


  implicit val modelManagementService: ModelManagementService = new ModelManagementServiceImpl

  implicit val runtimeDeployService: RuntimeDeployService = managerConfiguration.cloudDriver match {
    case c: SwarmCloudDriverConfiguration => new SwarmRuntimeDeployService(dockerClient, managerConfiguration)
    case c: DockerCloudDriverConfiguration => new DockerRuntimeDeployService(dockerClient, managerConfiguration)
    //TODO change
    case c: ECSCloudDriverConfiguration => new EcsRuntimeDeployService(c, managerConfiguration)
  }

  implicit val runtimeManagementService: RuntimeManagementService = new RuntimeManagementServiceImpl

  implicit val servingManagementService: ServingManagementService = new ServingManagementServiceImpl

  implicit val envoyManagementService = new EnvoyManagementServiceImpl

  implicit val envoyAdminConnector=new HttpEnvoyAdminConnector()

  implicit val prometheusMetricsService = new PrometheusMetricsServiceImpl

  implicit val uiManagementService = new UIManagementServiceImpl
}
