package io.hydrosphere.serving.streaming

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

/**
  *
  */
object StreamingKafkaBoot extends App with Logging {
  try {

    implicit val system = ActorSystem("streaming-kafka")
    implicit val materializer = ActorMaterializer()
    implicit val ex = system.dispatcher

    val conf = StreamingKafkaConfiguration.parse(ConfigFactory.load())

    val httpApi = new StreamingKafkaApi()
    val servingProcessor = SidecarServingProcessor(conf.sidecar, conf.streaming.processorRoute)
    val kafkaStream = {
      import conf.streaming._
      StreamingKafkaService.kafkaStream(sourceTopic, destinationTopic, servingProcessor)
    }

    kafkaStream.run().onComplete({
      case Success(_) =>
        system.terminate()
      case Failure(e) =>
        system.log.error(e, e.getMessage)
        system.terminate()
    })

    Http().bindAndHandle(httpApi.routes, "0.0.0.0", conf.application.port)

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

    logger.info(s"Started service on port: ${conf.application.port}")
  } catch {
    case e: Throwable =>
      logger.error("Fatal error", e)
      sys.exit(1)
  }
}