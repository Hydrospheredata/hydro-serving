package io.hydrosphere.serving.dummy_runtime

import java.io.File

import io.grpc.ServerBuilder
import io.hydrosphere.serving.tensorflow.api.prediction_service.PredictionServiceGrpc
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext
import scala.util.Try

object DummyRuntimeApp extends App with Logging {
  val dummyImpl = new DummyServiceImpl()
  val service = PredictionServiceGrpc.bindService(dummyImpl, ExecutionContext.global)

  val port = Try(sys.env("APP_PORT").toInt).getOrElse(9091)

  val server = ServerBuilder.forPort(port).addService(service).build()

  logger.info("Current models:")
  new File(Try(sys.env("MODEL_DIR")).getOrElse("/models"))
    .listFiles.foreach(f => logger.info(f.getAbsolutePath))

  server.start()
  logger.info(s"Server started! Port = ${server.getPort}")
  server.awaitTermination()
}