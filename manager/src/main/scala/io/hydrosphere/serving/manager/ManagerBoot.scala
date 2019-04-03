package io.hydrosphere.serving.manager

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cats.effect.{ContextShift, IO}
import cats.implicits._
import com.spotify.docker.client.DefaultDockerClient
import io.hydrosphere.serving.manager.discovery.{DiscoveryGrpc, DiscoveryHub}
import io.hydrosphere.serving.manager.api.grpc.GrpcApiServer
import io.hydrosphere.serving.manager.config.{DockerClientConfig, ManagerConfiguration}
import io.hydrosphere.serving.manager.api.http.HttpApiServer
import io.hydrosphere.serving.manager.domain.application.ApplicationService.Internals
import io.hydrosphere.serving.manager.domain.clouddriver.CloudDriver
import io.hydrosphere.serving.manager.grpc.entities.ServingApp
import io.hydrosphere.serving.manager.util.ReflectionUtils
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object ManagerBoot extends App with Logging {
  try {
    implicit val system: ActorSystem = ActorSystem("manager")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val timeout: Timeout = Timeout(5.minute)
    implicit val serviceExecutionContext: ExecutionContext = ExecutionContext.global
    implicit val contextShift: ContextShift[IO] = IO.contextShift(serviceExecutionContext)
    val configLoadResult = ManagerConfiguration.load
    val configuration = configLoadResult match {
      case Left(err) =>
        val textErr = err.toList.map(x => s"${x.location}: ${x.description}").mkString("\n")
        logger.error(s"Configuration errors:\n$textErr")
        throw new IllegalArgumentException(textErr)
      case Right(config) =>
        logger.info(s"Config loaded:\n${ReflectionUtils.prettyPrint(config)}")
        config
    }

    val dockerClient = DefaultDockerClient.fromEnv().build()
    val dockerClientConfig = {
      if (Files.exists(DockerClientConfig.defaultConfigPath)) {
        DockerClientConfig.load(DockerClientConfig.defaultConfigPath) match {
          case scala.util.Success(value) => value
          case scala.util.Failure(e) =>
            logger.warn(s"Failed to read docker config. Falling back to defaults", e)
            DockerClientConfig()
        }
      } else DockerClientConfig()
    }
    
    logger.info(s"Using docker client config: ${ReflectionUtils.prettyPrint(dockerClientConfig)}")

    val cloudDriver = CloudDriver.fromConfig[IO](configuration.cloudDriver, configuration.dockerRepository)
    val managerRepositories = new ManagerRepositories[IO](configuration)
    val discoveryHubIO = for {
      observed <- DiscoveryHub.observed[IO]
      instances <- cloudDriver.instances
      apps <- managerRepositories.applicationRepository.all()
      _ <- IO(logger.info(s"$instances"))
      needToDeploy = for {
        app <- apps
      } yield {
        val versions = app.executionGraph.stages.flatMap(_.modelVariants.map(_.modelVersion))
        val deployed = instances.filter(inst => versions.map(_.id).contains(inst.modelVersionId))
        IO.fromEither(Internals.toServingApp(app, deployed))
      }
      servingApps <- needToDeploy.toList.sequence[IO, ServingApp]
      _ <- servingApps.map(x => observed.added(x)).sequence
    } yield observed
    
    val discoveryHub = discoveryHubIO.unsafeRunSync()
    
    val managerServices = new ManagerServices[IO](
      discoveryHub,
      managerRepositories,
      configuration,
      dockerClient,
      dockerClientConfig,
      cloudDriver
    )
    
    
    val httpApi = new HttpApiServer(managerRepositories, managerServices, configuration)
    val grpcApi = GrpcApiServer(managerRepositories, managerServices, configuration, discoveryHub)

    httpApi.start() // fire and forget?
    grpcApi.start()

    sys addShutdownHook {
      grpcApi.shutdown()
      logger.info("Stopping all contexts")
      system.terminate()
      try {
        grpcApi.awaitTermination(30, TimeUnit.SECONDS)
        Await.ready(system.whenTerminated, Duration(30, TimeUnit.SECONDS))
      } catch {
        case e: Throwable =>
          logger.error("Error on terminate", e)
          sys.exit(1)
      }
    }

    logger.info(s"Started http service on port: ${configuration.application.port} and grpc service on ${configuration.application.grpcPort}")
  } catch {
    case e: Throwable =>
      logger.error("Fatal error", e)
      sys.exit(1)
  }
}
