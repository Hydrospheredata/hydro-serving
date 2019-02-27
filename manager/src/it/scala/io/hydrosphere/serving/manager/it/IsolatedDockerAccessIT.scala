package io.hydrosphere.serving.manager.it

import io.hydrosphere.serving.manager.util.docker.IsolatedDockerClient
import org.apache.logging.log4j.scala.Logging
import org.scalatest.{AsyncFunSpecLike, BeforeAndAfterAll}

trait IsolatedDockerAccessIT extends AsyncFunSpecLike with BeforeAndAfterAll with Logging {
  val dockerClient = IsolatedDockerClient.createFromEnv
  logger.info("Initialized IsolatedDockerClient")

  override protected def afterAll(): Unit = {
    logger.info("Cleaning up images and containers.")
    dockerClient.clear()
    super.beforeAll()
  }

  sys.addShutdownHook {
    dockerClient.clear()
  }
}