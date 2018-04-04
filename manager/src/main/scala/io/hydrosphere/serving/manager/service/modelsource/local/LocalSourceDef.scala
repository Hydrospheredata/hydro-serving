package io.hydrosphere.serving.manager.service.modelsource.local

import io.hydrosphere.serving.manager.model.{LocalSourceParams, ModelSourceConfig}
import io.hydrosphere.serving.manager.service.modelsource.SourceDef

case class LocalSourceDef(
  name: String,
  pathPrefix: Option[String]
) extends SourceDef

object LocalSourceDef{
  def fromConfig(localSourceConfig: ModelSourceConfig[LocalSourceParams]): LocalSourceDef =
    new LocalSourceDef(localSourceConfig.name, localSourceConfig.params.pathPrefix)
}