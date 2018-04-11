package io.hydrosphere.serving.manager.service.modelsource.local

import io.hydrosphere.serving.manager.model.db.ModelSourceConfig.LocalSourceParams
import io.hydrosphere.serving.manager.service.modelsource.SourceDef

case class LocalSourceDef(
  name: String,
  pathPrefix: Option[String]
) extends SourceDef

object LocalSourceDef{
  def fromConfig(name: String, localSourceParams: LocalSourceParams): LocalSourceDef =
    new LocalSourceDef(name, localSourceParams.pathPrefix)
}