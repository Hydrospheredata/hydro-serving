package io.hydrosphere.serving.manager

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.grpc.ServerBuilder
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{Await, ExecutionContext}
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
    val managerApi = new ManagerHttpApi(managerServices)
    val managerActors = new ManagerActors(managerServices, configuration)
    val managerGrpc = new ManagerGrpcApi(managerServices)

    val grpcServer = ServerBuilder
      .forPort(configuration.application.grpc.port)
      .addService(PredictionServiceGrpc.bindService(managerGrpc, ExecutionContext.global))
      .build()

    grpcServer.start()

    Http().bindAndHandle(managerApi.routes, "0.0.0.0", configuration.application.http.port)

    sys addShutdownHook {
      logger.info("Stopping all the contexts")
      system.terminate()
      grpcServer.shutdown()
      try {
        Await.ready(system.whenTerminated, Duration(30, TimeUnit.MINUTES))
        if (grpcServer.isShutdown) {
          grpcServer.shutdownNow()
        }
      } catch {
        case e: Throwable =>
          logger.error("Error on terminate", e)
          sys.exit(1)
      }
    }


    logger.info(s"Started HTTP service on port: ${configuration.application.http.port}")
    logger.info(s"Started gRPC service on port: ${configuration.application.grpc.port}")
  } catch {
    case e: Throwable =>
      logger.error("Fatal error", e)
      sys.exit(1)
  }
}
