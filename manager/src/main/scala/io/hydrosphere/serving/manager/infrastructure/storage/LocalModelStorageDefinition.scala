package io.hydrosphere.serving.manager.infrastructure.storage

import java.nio.file.Path

case class LocalModelStorageDefinition(
  name: String,
  path: Path
) extends ModelStorageDefinition
