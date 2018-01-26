package io.hydrosphere.serving.manager.connector.manager

import io.hydrosphere.serving.manager.model.Application

import scala.concurrent.Future


trait ManagerConnector {

  def getApplications: Future[Seq[Application]]
}
