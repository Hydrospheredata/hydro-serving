package io.hydrosphere.serving.manager.service.modelbuild.docker

import DockerClientHelper.ProgressMessage
import org.apache.logging.log4j.scala.Logger

trait ProgressHandler {
  def handle(progressMessage: ProgressMessage)
}

object ProgressHandler {

  class LoggerHandler(logger: Logger) extends ProgressHandler {
    override def handle(progressMessage: ProgressMessage): Unit =
      logger.info(progressMessage)
  }

}