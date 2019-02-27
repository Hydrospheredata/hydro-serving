package io.hydrosphere.serving.manager

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cats.effect.{ContextShift, IO}
import com.spotify.docker.client.DefaultDockerClient
import io.hydrosphere.serving.manager.api.grpc.GrpcApiServer
import io.hydrosphere.serving.manager.config.{DockerClientConfig, ManagerConfiguration}
import io.hydrosphere.serving.manager.api.http.HttpApiServer
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
    val dockerClientConfig = DockerClientConfig.load(DockerClientConfig.defaultConfigPath) match {
      case scala.util.Success(value) => value
      case scala.util.Failure(exception) =>
        logger.warn(s"Failed to read docker config. Falling back to defaults", exception)
        DockerClientConfig()
    }
    logger.info(s"Using docker client config: ${ReflectionUtils.prettyPrint(dockerClientConfig)}")

    val managerRepositories = new ManagerRepositories[IO](configuration)
    val managerServices = new ManagerServices[IO](managerRepositories, configuration, dockerClient, dockerClientConfig)
    val httpApi = new HttpApiServer(managerRepositories, managerServices, configuration)
    val grpcApi = GrpcApiServer(managerRepositories, managerServices, configuration)

    httpApi.start // fire and forget?
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
