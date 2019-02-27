package io.hydrosphere.serving.manager.domain.model

import io.hydrosphere.serving.manager.grpc.entities.{Model => GModel}

case class Model(
  id: Long,
  name: String
) {
  def toGrpc = GModel(
    id = id,
    name = name
  )
}
