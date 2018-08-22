package io.hydrosphere.serving.manager.model.db

import io.hydrosphere.serving.manager.grpc.entities.{Environment => GEnv}


case class Environment(
  id: Long,
  name: String,
  placeholders: Seq[Any]
) {
  def toGrpc = GEnv(
    id = id,
    name = name
  )
}