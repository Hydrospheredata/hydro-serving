package io.hydrosphere.serving.manager

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cats.effect.ConcurrentEffect
import com.spotify.docker.client._
import io.grpc._
import io.hydrosphere.serving.grpc.{AuthorityReplacerInterceptor, Headers}
import io.hydrosphere.serving.manager.config.{DockerClientConfig, ManagerConfiguration}
import io.hydrosphere.serving.manager.domain.application.ApplicationService
import io.hydrosphere.serving.manager.domain.clouddriver.CloudDriver
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorService
import io.hydrosphere.serving.manager.domain.image.ImageRepository
import io.hydrosphere.serving.manager.domain.model.ModelService
import io.hydrosphere.serving.manager.domain.model_build.ModelVersionBuilder
import io.hydrosphere.serving.manager.domain.model_version.ModelVersionService
import io.hydrosphere.serving.manager.domain.servable.ServableService
import io.hydrosphere.serving.manager.infrastructure.envoy.events.{ApplicationDiscoveryEventBus, CloudServiceDiscoveryEventBus, ServableDiscoveryEventBus}
import io.hydrosphere.serving.manager.infrastructure.envoy.{EnvoyGRPCDiscoveryService, XDSManagementActor}
import io.hydrosphere.serving.manager.infrastructure.image.DockerImageBuilder
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.ModelFetcher
import io.hydrosphere.serving.manager.infrastructure.storage.{LocalStorageOps, ModelUnpacker, StorageOps}
import io.hydrosphere.serving.manager.util.docker.InfoProgressHandler
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

class ManagerServices[F[_]: ConcurrentEffect](
  val managerRepositories: ManagerRepositories[F],
  val managerConfiguration: ManagerConfiguration,
  val dockerClient: DockerClient,
  val dockerClientConfig: DockerClientConfig
)(
  implicit val ex: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer,
  implicit val timeout: Timeout
) extends Logging {

  val progressHandler: ProgressHandler = InfoProgressHandler

  val managedChannel: ManagedChannel = ManagedChannelBuilder
    .forAddress(managerConfiguration.sidecar.host, managerConfiguration.sidecar.egressPort)
    .usePlaintext()
    .build

  val channel: Channel = ClientInterceptors.intercept(managedChannel, new AuthorityReplacerInterceptor +: Headers.interceptors: _*)

  val storageOps: LocalStorageOps[F] = StorageOps.default

  val modelStorage: ModelUnpacker[F] = ModelUnpacker[F](storageOps)

  val modelFetcher: ModelFetcher[F] = ModelFetcher.default[F](storageOps)

  val imageBuilder = new DockerImageBuilder(
    dockerClient = dockerClient,
    dockerClientConfig = dockerClientConfig,
    modelStorage = modelStorage,
    progressHandler = progressHandler
  )

  val imageRepository: ImageRepository[F] = ImageRepository.fromConfig(dockerClient, progressHandler, managerConfiguration.dockerRepository)

  val appEvent: ApplicationDiscoveryEventBus[F] = ApplicationDiscoveryEventBus.fromActorSystem[F](system)
  val servableEvent: ServableDiscoveryEventBus[F] = ServableDiscoveryEventBus.fromActorSystem[F](system)
  val cloudServiceEvent: CloudServiceDiscoveryEventBus[F] = CloudServiceDiscoveryEventBus.fromActorSystem[F](system)

  val hostSelectorService: HostSelectorService[F] = HostSelectorService[F](managerRepositories.hostSelectorRepository)

  val versionService: ModelVersionService[F] = ModelVersionService[F](
    modelVersionRepository = managerRepositories.modelVersionRepository,
    applicationRepo = managerRepositories.applicationRepository,
  )

  val versionBuilder = ModelVersionBuilder(
    imageBuilder = imageBuilder,
    modelVersionRepository = managerRepositories.modelVersionRepository,
    imageRepository = imageRepository,
    modelVersionService = versionService,
    storageOps = storageOps
  )

  val cloudDriverService: CloudDriver[F] = CloudDriver.fromConfig[F](
    dockerClient = dockerClient,
    eventPublisher = cloudServiceEvent,
    cloudDriverConfiguration = managerConfiguration.cloudDriver,
    applicationConfiguration = managerConfiguration.application,
    advertisedConfiguration = managerConfiguration.manager,
    dockerRepositoryConfiguration = managerConfiguration.dockerRepository,
    sidecarConfig = managerConfiguration.sidecar
  )

  logger.info(s"Using ${cloudDriverService.getClass} cloud driver")

  val servableService: ServableService[F] = ServableService[F](
    cloudDriverService,
    managerRepositories.servableRepository,
    servableEvent
  )

  val appService: ApplicationService[F] = ApplicationService[F](
    applicationRepository = managerRepositories.applicationRepository,
    versionRepository = managerRepositories.modelVersionRepository,
    servableRepo = managerRepositories.servableRepository,
    servableService = servableService,
    appEvents = appEvent
  )

  val modelService: ModelService[F] = ModelService[F](
    modelRepository = managerRepositories.modelRepository,
    modelVersionService = versionService,
    modelVersionRepository = managerRepositories.modelVersionRepository,
    storageService = modelStorage,
    appRepo = managerRepositories.applicationRepository,
    hostSelectorRepository = managerRepositories.hostSelectorRepository,
    fetcher = modelFetcher,
    modelVersionBuilder = versionBuilder
  )

  val xdsActor: ActorRef = XDSManagementActor.makeXdsActor(
    cloudDriver = cloudDriverService,
    servableService = servableService,
    applicationRepository = managerRepositories.applicationRepository
  )

  val envoyGRPCDiscoveryService: EnvoyGRPCDiscoveryService[F] = EnvoyGRPCDiscoveryService.actorManaged(
    xdsActor = xdsActor,
    servableService = servableService,
    appService = appService
  )
}