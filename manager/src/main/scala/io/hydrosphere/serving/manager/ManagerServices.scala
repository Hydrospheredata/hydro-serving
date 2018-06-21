package io.hydrosphere.serving.manager

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.paulgoldbaum.influxdbclient._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.spotify.docker.client._
import io.grpc._
import io.hydrosphere.serving.grpc.{AuthorityReplacerInterceptor, Headers}
import io.hydrosphere.serving.manager.connector.HttpEnvoyAdminConnector
import io.hydrosphere.serving.manager.service.aggregated_info.{AggregatedInfoUtilityService, AggregatedInfoUtilityServiceImpl}
import io.hydrosphere.serving.manager.service.application.{ApplicationManagementService, ApplicationManagementServiceImpl}
import io.hydrosphere.serving.manager.service.build_script.{BuildScriptManagementService, BuildScriptManagementServiceImpl}
import io.hydrosphere.serving.manager.service.clouddriver._
import io.hydrosphere.serving.manager.service.contract.ContractUtilityServiceImpl
import io.hydrosphere.serving.manager.service.environment.{EnvironmentManagementService, EnvironmentManagementServiceImpl}
import io.hydrosphere.serving.manager.service.envoy.{EnvoyGRPCDiscoveryService, EnvoyGRPCDiscoveryServiceImpl}
import io.hydrosphere.serving.manager.service.internal_events.InternalManagerEventsPublisher
import io.hydrosphere.serving.manager.service.metrics.{ElasticSearchMetricsService, InfluxDBMetricsService, PrometheusMetricsServiceImpl}
import io.hydrosphere.serving.manager.service.model.{ModelManagementService, ModelManagementServiceImpl}
import io.hydrosphere.serving.manager.service.model_build.builders._
import io.hydrosphere.serving.manager.service.model_build.{ModelBuildManagementServiceImpl, ModelBuildManagmentService}
import io.hydrosphere.serving.manager.service.model_version.{ModelVersionManagementService, ModelVersionManagementServiceImpl}
import io.hydrosphere.serving.manager.service.runtime.{DefaultRuntimes, RuntimeManagementService, RuntimeManagementServiceImpl}
import io.hydrosphere.serving.manager.service.service.{ServiceManagementService, ServiceManagementServiceImpl}
import io.hydrosphere.serving.manager.service.source.ModelStorageServiceImpl
import io.hydrosphere.serving.monitoring.monitoring.MonitoringServiceGrpc
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

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
    .usePlaintext()
    .build

  val channel: Channel = ClientInterceptors.intercept(managedChannel, new AuthorityReplacerInterceptor +: Headers.interceptors: _*)

  val servingMeshGrpcClient: PredictionServiceGrpc.PredictionServiceStub = PredictionServiceGrpc.stub(channel)
  val monitoringServiceClient: MonitoringServiceGrpc.MonitoringServiceStub = MonitoringServiceGrpc.stub(channel)

  val sourceManagementService = new ModelStorageServiceImpl(managerConfiguration)

  val contractUtilityService = new ContractUtilityServiceImpl

  val modelBuildService: ModelBuildService = new LocalModelBuildService(dockerClient, sourceManagementService)

  val modelPushService: ModelPushService = managerConfiguration.dockerRepository match {
    case c: ECSDockerRepositoryConfiguration => new ECSModelPushService(dockerClient, c)
    case _ => new EmptyModelPushService
  }

  val internalManagerEventsPublisher = new InternalManagerEventsPublisher

  val modelManagementService: ModelManagementService = new ModelManagementServiceImpl(
    managerRepositories.modelRepository,
    managerRepositories.modelVersionRepository,
    sourceManagementService,
    contractUtilityService
  )

  val buildScriptManagementService: BuildScriptManagementService = new BuildScriptManagementServiceImpl(
    managerRepositories.modelBuildScriptRepository
  )

  val modelVersionManagementService: ModelVersionManagementService = new ModelVersionManagementServiceImpl(
    managerRepositories.modelVersionRepository,
    modelManagementService,
    contractUtilityService
  )

  val modelBuildManagmentService: ModelBuildManagmentService = new ModelBuildManagementServiceImpl(
    managerRepositories.modelBuildRepository,
    buildScriptManagementService,
    modelVersionManagementService,
    modelManagementService,
    modelPushService,
    modelBuildService
  )

  val cloudDriverService: CloudDriverService = managerConfiguration.cloudDriver match {
    case _: ECSCloudDriverConfiguration => new ECSCloudDriverService(managerConfiguration, internalManagerEventsPublisher)
    case _: DockerCloudDriverConfiguration => new DockerComposeCloudDriverService(dockerClient, managerConfiguration, internalManagerEventsPublisher)
    case _ => new LocalCloudDriverService(dockerClient, managerConfiguration, internalManagerEventsPublisher)
  }

  val runtimeManagementService: RuntimeManagementService = new RuntimeManagementServiceImpl(
    managerRepositories.runtimeRepository,
    dockerClient
  )
  managerConfiguration.runtimesStarterPack.foreach(runtimeManagementService.create)

  val environmentManagementService: EnvironmentManagementService = new EnvironmentManagementServiceImpl(managerRepositories.environmentRepository)

  val serviceManagementService: ServiceManagementService = new ServiceManagementServiceImpl(
    cloudDriverService,
    managerRepositories.serviceRepository,
    runtimeManagementService,
    modelVersionManagementService,
    environmentManagementService,
    internalManagerEventsPublisher
  )

  val applicationManagementService: ApplicationManagementService = new ApplicationManagementServiceImpl(
    applicationRepository = managerRepositories.applicationRepository,
    modelVersionManagementService = modelVersionManagementService,
    serviceManagementService = serviceManagementService,
    grpcClient = servingMeshGrpcClient,
    grpcClientForMonitoring = monitoringServiceClient,
    internalManagerEventsPublisher = internalManagerEventsPublisher,
    applicationConfig = managerConfiguration.application,
    runtimeRepository = managerRepositories.runtimeRepository
  )

  val aggregatedInfoUtilityService: AggregatedInfoUtilityService = new AggregatedInfoUtilityServiceImpl(
    modelManagementService,
    modelBuildManagmentService,
    modelVersionManagementService,
    applicationManagementService
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

  managerConfiguration.metrics.elasticSearch.foreach { conf =>
    val elasticClient = HttpClient(ElasticsearchClientUri(conf.clientUri))

    val elasticActor = system.actorOf(ElasticSearchMetricsService.props(
      managerConfiguration: ManagerConfiguration,
      envoyAdminConnector,
      cloudDriverService,
      serviceManagementService,
      applicationManagementService,
      elasticClient
    ))
  }

  managerConfiguration.metrics.influxDB.foreach { conf =>
    val influxDBClient = InfluxDB.connect(conf.host, conf.port)

    val influxActor = system.actorOf(InfluxDBMetricsService.props(
      managerConfiguration,
      envoyAdminConnector,
      cloudDriverService,
      serviceManagementService,
      applicationManagementService,
      influxDBClient
    ))
  }
}
