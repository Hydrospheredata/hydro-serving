package io.hydrosphere.serving.manager.util.docker

import com.spotify.docker.client.ProgressHandler
import com.spotify.docker.client.messages.ProgressMessage

import scala.collection.mutable.ListBuffer

class HistoricProgressHandler extends ProgressHandler {
  private val messageList = ListBuffer.empty[ProgressMessage]

  override def progress(progressMessage: ProgressMessage): Unit = {
    messageList += progressMessage
  }

  def messages = messageList.toList
}
