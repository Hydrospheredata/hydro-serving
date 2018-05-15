package io.hydrosphere.serving.manager.service.source.sources.local

import io.hydrosphere.serving.manager.service.source.sources.SourceDef

case class LocalSourceDef(
  name: String,
  pathPrefix: String
) extends SourceDef
