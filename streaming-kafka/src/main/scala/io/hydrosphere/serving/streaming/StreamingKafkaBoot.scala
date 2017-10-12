package io.hydrosphere.serving.streaming

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  *
  */
object StreamingKafkaBoot extends App with Logging {
  try {
    implicit val system = ActorSystem("streaming-kafka")
    implicit val materializer = ActorMaterializer()
    implicit val ex = system.dispatcher

    val configuration = StreamingKafkaConfiguration.parse(ConfigFactory.load())

    val httpApi = new StreamingKafkaApi()
    val streamingKafkaService = new StreamingKafkaService(configuration)

    Http().bindAndHandle(httpApi.routes, "0.0.0.0", configuration.application.port)

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