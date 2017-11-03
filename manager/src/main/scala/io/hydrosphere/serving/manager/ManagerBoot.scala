package io.hydrosphere.serving.manager

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  *
  */
object ManagerBoot extends App with Logging {
  try {
    implicit val system = ActorSystem("manager")
    implicit val materializer = ActorMaterializer()
    implicit val ex = system.dispatcher
    implicit val timeout = Timeout(5.minute)

    val configuration = ManagerConfiguration.parse(ConfigFactory.load())

    val managerRepositories = new ManagerRepositoriesConfig(configuration)
    val managerServices = new ManagerServices(managerRepositories, configuration)
    val managerApi = new ManagerApi(managerServices)
    val managerActors = new ManagerActors(managerServices)


    Http().bindAndHandle(managerApi.routes, "0.0.0.0", configuration.application.port)

    sys addShutdownHook {
      logger.info("Stopping all the contexts")
      system.terminate()
      try {
        Await.ready(system.whenTerminated, Duration(30, TimeUnit.MINUTES))
      } catch {
        case e: Throwable =>
          logger.error("Error on terminate", e)
          sys.exit(1)
      }
    }

    logger.info(s"Started service on port: ${configuration.application.port}")
  } catch {
    case e: Throwable =>
      logger.error("Fatal error", e)
      sys.exit(1)
  }
}
