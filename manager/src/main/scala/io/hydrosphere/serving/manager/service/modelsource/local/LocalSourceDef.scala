package io.hydrosphere.serving.manager.service.modelsource.local

import io.hydrosphere.serving.manager.model.{LocalSourceParams, ModelSourceConfig}
import io.hydrosphere.serving.manager.service.modelsource.SourceDef

case class LocalSourceDef(
  name: String,
  path: String
) extends SourceDef {
  override def prefix = name
}

object LocalSourceDef{
  def fromConfig(localModelSourceConfiguration: ModelSourceConfig[LocalSourceParams]): LocalSourceDef =
    new LocalSourceDef(localModelSourceConfiguration.name, localModelSourceConfiguration.params.path)
}