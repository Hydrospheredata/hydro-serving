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
import io.hydrosphere.serving.manager.config.{CloudDriverConfiguration, DockerClientConfig, DockerRepositoryConfiguration, ManagerConfiguration}
import io.hydrosphere.serving.manager.domain.application.ApplicationService
import io.hydrosphere.serving.manager.domain.build_script.BuildScriptService
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorService
import io.hydrosphere.serving.manager.domain.metrics.{ElasticSearchMetricsService, InfluxDBMetricsService, PrometheusMetricsService}
import io.hydrosphere.serving.manager.domain.model.ModelService
import io.hydrosphere.serving.manager.domain.model_version.ModelVersionService
import io.hydrosphere.serving.manager.domain.service.ServiceManagementService
import io.hydrosphere.serving.manager.infrastructure.clouddriver.{DockerComposeCloudDriverService, ECSCloudDriverService, KubernetesCloudDriverService, LocalCloudDriverService}
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.InternalManagerEventsPublisher
import io.hydrosphere.serving.manager.infrastructure.envoy.{EnvoyGRPCDiscoveryService, EnvoyGRPCDiscoveryServiceImpl, HttpEnvoyAdminConnector}
import io.hydrosphere.serving.manager.infrastructure.model.build.LocalModelBuildService
import io.hydrosphere.serving.manager.infrastructure.model.push._
import io.hydrosphere.serving.manager.infrastructure.storage.ModelStorageServiceImpl
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

  val managedChannel = ManagedChannelBuilder
    .forAddress(managerConfiguration.sidecar.host, managerConfiguration.sidecar.egressPort)
    .usePlaintext()
    .build

  val channel: Channel = ClientInterceptors.intercept(managedChannel, new AuthorityReplacerInterceptor +: Headers.interceptors: _*)

  val sourceManagementService = new ModelStorageServiceImpl(managerConfiguration)

  val modelBuildService = new LocalModelBuildService(dockerClient, dockerClientConfig, sourceManagementService)

  val buildScriptManagementService = new BuildScriptService(managerRepositories.modelBuildScriptRepository)

  val modelPushService = managerConfiguration.dockerRepository match {
    case c: DockerRepositoryConfiguration.Remote => new RemoteModelPushService(dockerClient, c)
    case c: DockerRepositoryConfiguration.Ecs => new ECSModelPushService(dockerClient, c)
    case _ => new LocalModelPushService
  }

  val internalManagerEventsPublisher = new InternalManagerEventsPublisher

  val hostSelectorService = new HostSelectorService(managerRepositories.hostSelectorRepository)

  val modelVersionManagementService = new ModelVersionService(
    managerRepositories.modelVersionRepository,
    buildScriptManagementService,
    hostSelectorService,
    modelBuildService,
    modelPushService,
    managerRepositories.applicationRepository
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
    internalManagerEventsPublisher = internalManagerEventsPublisher,
    applicationConfig = managerConfiguration.application,
    environmentManagementService = hostSelectorService
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
