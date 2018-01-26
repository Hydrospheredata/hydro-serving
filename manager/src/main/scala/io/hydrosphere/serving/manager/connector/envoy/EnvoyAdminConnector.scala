package io.hydrosphere.serving.manager.connector.envoy

import scala.concurrent.Future

trait EnvoyAdminConnector {

  def stats(host: String, port: Int): Future[String]

}
