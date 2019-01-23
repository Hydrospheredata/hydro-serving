package io.hydrosphere.serving.manager

import java.nio.file.Files

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.paulgoldbaum.influxdbclient._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.HttpClient
import com.spotify.docker.client._
import com.spotify.docker.client.messages.ProgressMessage
import io.grpc._
import io.hydrosphere.serving.grpc.{AuthorityReplacerInterceptor, Headers}
import io.hydrosphere.serving.manager.config.{CloudDriverConfiguration, DockerClientConfig, DockerRepositoryConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.domain.application.ApplicationService
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorService
import io.hydrosphere.serving.manager.domain.metrics.{ElasticSearchMetricsService, InfluxDBMetricsService, PrometheusMetricsService}
import io.hydrosphere.serving.manager.domain.model.ModelService
import io.hydrosphere.serving.manager.domain.model_version.ModelVersionService
import io.hydrosphere.serving.manager.domain.service.ServiceManagementService
import io.hydrosphere.serving.manager.infrastructure.clouddriver.{DockerComposeCloudDriverService, ECSCloudDriverService, KubernetesCloudDriverService, LocalCloudDriverService}
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.ManagerEventBus
import io.hydrosphere.serving.manager.infrastructure.envoy.{EnvoyGRPCDiscoveryService, EnvoyGRPCDiscoveryServiceImpl, HttpEnvoyAdminConnector}
import io.hydrosphere.serving.manager.infrastructure.image.DockerImageBuilder
import io.hydrosphere.serving.manager.infrastructure.image.repositories.{ECSImageRepository, LocalImageRepository, RemoteImageRepository}
import io.hydrosphere.serving.manager.infrastructure.model_build.TempFilePacker
import io.hydrosphere.serving.manager.infrastructure.storage.{LocalModelStorage, LocalStorageOps, LocalModelStorage}
import io.hydrosphere.serving.manager.util.docker.InfoProgressHandler
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

class ManagerServices(
  val managerRepositories: ManagerRepositories,
  val managerConfiguration: ManagerConfiguration,
  val dockerClient: DockerClient,
  val dockerClientConfig: DockerClientConfig
)(
  implicit val ex: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer,
  implicit val timeout: Timeout
) extends Logging {

  val progressHandler = InfoProgressHandler

  val managedChannel = ManagedChannelBuilder
    .forAddress(managerConfiguration.sidecar.host, managerConfiguration.sidecar.egressPort)
    .usePlaintext()
    .build

  val channel: Channel = ClientInterceptors.intercept(managedChannel, new AuthorityReplacerInterceptor +: Headers.interceptors: _*)

  val rootDir = managerConfiguration.localStorage.getOrElse(Files.createTempDirectory("hydroservingLocalStorage"))
  val storageOps = new LocalStorageOps
  logger.info(s"Using model storage: $rootDir")


  val sourceManagementService = new LocalModelStorage(
    rootDir = rootDir,
    storage = storageOps
  )

  val modelFilePacker = new TempFilePacker(sourceManagementService)

  val imageBuilder = new DockerImageBuilder(
    dockerClient = dockerClient,
    dockerClientConfig = dockerClientConfig,
    modelStorage = sourceManagementService,
    progressHandler = progressHandler
  )

  val imageRepository = managerConfiguration.dockerRepository match {
    case c: DockerRepositoryConfiguration.Remote => new RemoteImageRepository(dockerClient, c, progressHandler)
    case c: DockerRepositoryConfiguration.Ecs => new ECSImageRepository(dockerClient, c, progressHandler)
    case _ => new LocalImageRepository
  }

  val internalManagerEventsPublisher = ManagerEventBus.fromActorSystem(system)

  val hostSelectorService = new HostSelectorService(managerRepositories.hostSelectorRepository)

  val modelVersionManagementService = new ModelVersionService(
    modelVersionRepository = managerRepositories.modelVersionRepository,
    hostSelectorService = hostSelectorService,
    imageBuilder = imageBuilder,
    imageRepository = imageRepository,
    applicationRepo = managerRepositories.applicationRepository,
    modelFilePacker =
  )

  val cloudDriverService = managerConfiguration.cloudDriver match {
    case _: CloudDriverConfiguration.Ecs => new ECSCloudDriverService(managerConfiguration, internalManagerEventsPublisher)
    case _: CloudDriverConfiguration.Docker => new DockerComposeCloudDriverService(dockerClient, managerConfiguration, internalManagerEventsPublisher)
    case _: CloudDriverConfiguration.Kubernetes => new KubernetesCloudDriverService(managerConfiguration, internalManagerEventsPublisher)
    case _ => new LocalCloudDriverService(dockerClient, managerConfiguration, internalManagerEventsPublisher)
  }

  val serviceManagementService: ServiceManagementService = new ServiceManagementService(
    cloudDriverService,
    managerRepositories.serviceRepository,
    modelVersionManagementService,
    hostSelectorService,
    internalManagerEventsPublisher
  )

  val applicationManagementService = new ApplicationService(
    applicationRepository = managerRepositories.applicationRepository,
    versionRepository = managerRepositories.modelVersionRepository,
    serviceManagementService = serviceManagementService,
    internalManagerEventsPublisher = internalManagerEventsPublisher
  )

  val modelManagementService = new ModelService(
    managerRepositories.modelRepository,
    modelVersionManagementService,
    sourceManagementService,
    managerRepositories.applicationRepository,
    managerRepositories.hostSelectorRepository
  )

  val envoyGRPCDiscoveryService: EnvoyGRPCDiscoveryService = new EnvoyGRPCDiscoveryServiceImpl(
    serviceManagementService,
    applicationManagementService,
    managerConfiguration
  )

  val envoyAdminConnector = new HttpEnvoyAdminConnector()

  val prometheusMetricsService = new PrometheusMetricsService(
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

  managerConfiguration.metrics.influxDb.foreach { conf =>
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
