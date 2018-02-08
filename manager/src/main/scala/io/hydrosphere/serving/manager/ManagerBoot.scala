package io.hydrosphere.serving.manager

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.spotify.docker.client.DefaultDockerClient
import com.typesafe.config.ConfigFactory
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Await
import scala.concurrent.duration._

object ManagerBoot extends App with Logging {
  try {
    implicit val system = ActorSystem("manager")
    implicit val materializer = ActorMaterializer()
    implicit val ex = system.dispatcher
    implicit val timeout = Timeout(5.minute)

    val configuration = ManagerConfiguration.parse(ConfigFactory.load())

    val dockerClient = DefaultDockerClient.fromEnv().build() // move to config?

    val managerRepositories = new ManagerRepositoriesConfig(configuration)
    val managerServices = new ManagerServices(managerRepositories, configuration, dockerClient)
    val managerApi = new ManagerHttpApi(managerServices, configuration)

    val managerGRPC = new ManagerGRPC(managerServices, configuration)

    sys addShutdownHook {
      managerGRPC.server.shutdown()
      logger.info("Stopping all contexts")
      system.terminate()
      try {
        managerGRPC.server.awaitTermination(30, TimeUnit.SECONDS)
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
