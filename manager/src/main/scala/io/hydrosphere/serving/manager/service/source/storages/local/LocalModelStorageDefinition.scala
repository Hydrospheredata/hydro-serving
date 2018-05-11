package io.hydrosphere.serving.manager.service.source.storages.local

import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.LocalSourceParams
import io.hydrosphere.serving.manager.service.source.storages.ModelStorageDefinition

case class LocalModelStorageDefinition(
  name: String,
  pathPrefix: Option[String]
) extends ModelStorageDefinition

object LocalModelStorageDefinition{
  def fromConfig(name: String, localSourceParams: LocalSourceParams): LocalModelStorageDefinition =
    new LocalModelStorageDefinition(name, localSourceParams.pathPrefix)
}