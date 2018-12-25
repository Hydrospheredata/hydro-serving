package io.hydrosphere.serving.manager.domain.model

case class CreateModelRequest(
  name: String,
)

case class UpdateModelRequest(
  id: Long,
  name: String,
)