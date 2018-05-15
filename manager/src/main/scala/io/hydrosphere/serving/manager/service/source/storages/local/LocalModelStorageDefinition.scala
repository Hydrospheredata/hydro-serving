package io.hydrosphere.serving.manager.service.source.storages.local

import java.nio.file.Path

import io.hydrosphere.serving.manager.service.source.storages.ModelStorageDefinition

case class LocalModelStorageDefinition(
  name: String,
  path: Path
) extends ModelStorageDefinition
