package io.hydrosphere.serving.manager.util.docker

import com.spotify.docker.client.ProgressHandler
import com.spotify.docker.client.messages.ProgressMessage
import org.apache.logging.log4j.scala.Logging

object InfoProgressHandler extends ProgressHandler with Logging {
  override def progress(progressMessage: ProgressMessage): Unit =
    logger.debug(progressMessage)
}
