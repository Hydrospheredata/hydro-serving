package io.hydrosphere.serving.manager

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.spotify.docker.client._
import io.grpc.{Channel, ClientInterceptors, ManagedChannelBuilder}
import io.hydrosphere.serving.grpc.{AuthorityReplacerInterceptor, KafkaTopicServerInterceptor}
import io.hydrosphere.serving.manager.connector.HttpEnvoyAdminConnector
import io.hydrosphere.serving.manager.service.clouddriver._
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.actors.RepositoryIndexActor
import io.hydrosphere.serving.manager.service.envoy.{EnvoyGRPCDiscoveryService, EnvoyGRPCDiscoveryServiceImpl}
import io.hydrosphere.serving.manager.service.modelbuild._
import io.hydrosphere.serving.manager.service.prometheus.PrometheusMetricsServiceImpl
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

/**
  *
  */
class ManagerServices(
  val managerRepositories: ManagerRepositories,
  val managerConfiguration: ManagerConfiguration,
  val dockerClient: DockerClient
)(
  implicit val ex: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer,
  implicit val timeout: Timeout
) extends Logging {

  val managedChannel = ManagedChannelBuilder
    .forAddress(managerConfiguration.sidecar.host, managerConfiguration.sidecar.egressPort)
    .usePlaintext(true)
    .build

  val channel: Channel = ClientInterceptors
    .intercept(managedChannel, new AuthorityReplacerInterceptor, new KafkaTopicServerInterceptor)

  val servingMeshGrpcClient: PredictionServiceGrpc.PredictionServiceStub = PredictionServiceGrpc.stub(channel)

  val sourceManagementService = new SourceManagementServiceImpl(managerConfiguration, managerRepositories.sourceRepository)

  sourceManagementService.createWatchers

  val repoActor: ActorRef = system.actorOf(RepositoryIndexActor.props(managerRepositories.modelRepository))

  val modelBuildService: ModelBuildService = new LocalModelBuildService(dockerClient, sourceManagementService)

  val modelPushService: ModelPushService = managerConfiguration.dockerRepository match {
    case c: ECSDockerRepositoryConfiguration => new ECSModelPushService(dockerClient, c)
    case _ => new EmptyModelPushService
  }

  val internalManagerEventsPublisher = new InternalManagerEventsPublisher

  val modelManagementService: ModelManagementService = new ModelManagementServiceImpl(
    managerRepositories.modelRepository,
    managerRepositories.modelVersionRepository,
    managerRepositories.modelBuildRepository,
    managerRepositories.modelBuildScriptRepository,
    modelBuildService,
    modelPushService,
    sourceManagementService,
    repoActor
  )

  val cloudDriverService: CloudDriverService = managerConfiguration.cloudDriver match {
    case _: DockerCloudDriverConfiguration => new DockerComposeCloudDriverService(dockerClient, managerConfiguration, internalManagerEventsPublisher)
    case _ => new LocalCloudDriverService(dockerClient, managerConfiguration, internalManagerEventsPublisher)

  }

  val runtimeManagementService: RuntimeManagementService = new RuntimeManagementServiceImpl(managerRepositories.runtimeRepository)

  val serviceManagementService: ServiceManagementService = new ServiceManagementServiceImpl(
    cloudDriverService,
    managerRepositories.serviceRepository,
    managerRepositories.runtimeRepository,
    managerRepositories.modelVersionRepository,
    managerRepositories.environmentRepository,
    internalManagerEventsPublisher
  )

  val applicationManagementService: ApplicationManagementService = new ApplicationManagementServiceImpl(
    applicationRepository = managerRepositories.applicationRepository,
    serviceManagementService = serviceManagementService,
    grpcClient = servingMeshGrpcClient,
    internalManagerEventsPublisher = internalManagerEventsPublisher,
    modelVersionRepository = managerRepositories.modelVersionRepository,
    applicationConfig = managerConfiguration.application,
    runtimeRepository = managerRepositories.runtimeRepository
  )

  val envoyGRPCDiscoveryService: EnvoyGRPCDiscoveryService = new EnvoyGRPCDiscoveryServiceImpl(
    serviceManagementService,
    applicationManagementService,
    cloudDriverService,
    managerConfiguration
  )

  val envoyAdminConnector = new HttpEnvoyAdminConnector()

  val prometheusMetricsService = new PrometheusMetricsServiceImpl(
    cloudDriverService,
    envoyAdminConnector,
    serviceManagementService,
    applicationManagementService
  )
}
