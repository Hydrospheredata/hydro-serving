package io.hydrosphere.serving.manager

import java.nio.file.Files

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cats.effect.Effect
import com.spotify.docker.client._
import io.grpc._
import io.hydrosphere.serving.grpc.{AuthorityReplacerInterceptor, Headers}
import io.hydrosphere.serving.manager.config.{DockerClientConfig, ManagerConfiguration}
import io.hydrosphere.serving.manager.domain.application.ApplicationService
import io.hydrosphere.serving.manager.domain.clouddriver.CloudDriver
import io.hydrosphere.serving.manager.domain.host_selector.HostSelectorService
import io.hydrosphere.serving.manager.domain.image.ImageRepository
import io.hydrosphere.serving.manager.domain.model.ModelService
import io.hydrosphere.serving.manager.domain.model_version.ModelVersionService
import io.hydrosphere.serving.manager.domain.servable.ServableService
import io.hydrosphere.serving.manager.infrastructure.envoy.internal_events.ManagerEventBus
import io.hydrosphere.serving.manager.infrastructure.envoy.{EnvoyGRPCDiscoveryService, XDSManagementActor}
import io.hydrosphere.serving.manager.infrastructure.image.DockerImageBuilder
import io.hydrosphere.serving.manager.infrastructure.model_build.TempFilePacker
import io.hydrosphere.serving.manager.infrastructure.storage.fetchers.ModelFetcher
import io.hydrosphere.serving.manager.infrastructure.storage.{LocalModelStorage, LocalStorageOps, StorageOps}
import io.hydrosphere.serving.manager.util.docker.InfoProgressHandler
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext

class ManagerServices[F[_]: Effect](
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

  private val rootDir = managerConfiguration.localStorage.getOrElse(Files.createTempDirectory("hydroservingLocalStorage"))
  val storageOps: LocalStorageOps[F] = StorageOps.default
  logger.info(s"Using model storage: $rootDir")


  val modelStorage = new LocalModelStorage[F](
    rootDir = rootDir,
    storageOps = storageOps
  )

  val modelFetcher: ModelFetcher[F] = ModelFetcher.default[F](storageOps)

  val modelFilePacker = new TempFilePacker(
    modelStorage = modelStorage,
    storageOps = storageOps
  )

  val imageBuilder = new DockerImageBuilder(
    dockerClient = dockerClient,
    dockerClientConfig = dockerClientConfig,
    modelStorage = modelStorage,
    progressHandler = progressHandler
  )

  val imageRepository: ImageRepository[F] = ImageRepository.fromConfig(dockerClient, progressHandler, managerConfiguration.dockerRepository)

  val eventPublisher: ManagerEventBus[F] = ManagerEventBus.fromActorSystem[F](system)

  val hostSelectorService: HostSelectorService[F] = HostSelectorService[F](managerRepositories.hostSelectorRepository)

  val versionService: ModelVersionService[F] = ModelVersionService[F](
    modelVersionRepository = managerRepositories.modelVersionRepository,
    imageBuilder = imageBuilder,
    imageRepository = imageRepository,
    applicationRepo = managerRepositories.applicationRepository,
    modelFilePacker = modelFilePacker
  )

  val cloudDriverService: CloudDriver[F] = CloudDriver.fromConfig[F](
    dockerClient = dockerClient,
    eventPublisher = eventPublisher,
    cloudDriverConfiguration = managerConfiguration.cloudDriver,
    applicationConfiguration = managerConfiguration.application,
    advertisedConfiguration = managerConfiguration.manager,
    dockerRepositoryConfiguration = managerConfiguration.dockerRepository,
    sidecarConfig = managerConfiguration.sidecar
  )

  val servableService: ServableService[F] = ServableService[F](
    cloudDriverService,
    managerRepositories.servableRepository,
    eventPublisher
  )

  val appService: ApplicationService[F] = ApplicationService[F](
    applicationRepository = managerRepositories.applicationRepository,
    versionRepository = managerRepositories.modelVersionRepository,
    servableRepo = managerRepositories.servableRepository,
    servableService = servableService,
    internalManagerEventsPublisher = eventPublisher
  )

  val modelService: ModelService[F] = ModelService[F](
    modelRepository = managerRepositories.modelRepository,
    modelVersionService = versionService,
    modelVersionRepository = managerRepositories.modelVersionRepository,
    storageService = modelStorage,
    appRepo = managerRepositories.applicationRepository,
    hostSelectorRepository = managerRepositories.hostSelectorRepository,
    fetcher = modelFetcher
  )

  val xdsActor: ActorRef = XDSManagementActor.makeXdsActor(
    servableService = servableService,
    appService = appService
  )

  val envoyGRPCDiscoveryService: EnvoyGRPCDiscoveryService[F] = EnvoyGRPCDiscoveryService.actorManaged(
    xdsActor = xdsActor,
    servableService = servableService,
    appService = appService
  )

}