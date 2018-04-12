package io.hydrosphere.serving.manager.service.source.sources

import java.io.FileNotFoundException

import io.hydrosphere.serving.manager.service.source.sources.local.{LocalModelSource, LocalSourceDef}

class LocalSourceSpecs extends ModelSourceSpec(new LocalModelSource(LocalSourceDef("test", None))) {
  override def getSourceFile(path: String): String = {
    val resPath = "/test_models/" + path
    Option(getClass.getResource(resPath))
      .map(_.getPath)
      .getOrElse(throw new FileNotFoundException(s"$resPath not found in resources"))
  }
}
