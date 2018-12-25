package io.hydrosphere.serving.manager

import java.nio.file.Paths

object TestConstants {
  val localModelsPath = Paths.get(this.getClass.getClassLoader.getResource("test_models").toURI)
}