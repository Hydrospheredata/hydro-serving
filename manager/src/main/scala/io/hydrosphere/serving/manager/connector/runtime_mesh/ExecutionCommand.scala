package io.hydrosphere.serving.manager.connector.runtime_mesh

import akka.http.scaladsl.model.HttpHeader

case class ExecutionCommand(
  headers: Seq[HttpHeader],
  json: Array[Byte],
  pipe: Seq[ExecutionUnit]
)
