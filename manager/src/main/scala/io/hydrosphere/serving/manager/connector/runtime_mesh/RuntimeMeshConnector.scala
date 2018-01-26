package io.hydrosphere.serving.manager.connector.runtime_mesh

import scala.concurrent.Future

trait RuntimeMeshConnector {
  def execute(command: ExecutionCommand): Future[ExecutionResult]
}
