package io.hydrosphere.serving.manager.model.db

case class Environment(
  id: Long,
  name: String,
  placeholders: Seq[Any]
)
