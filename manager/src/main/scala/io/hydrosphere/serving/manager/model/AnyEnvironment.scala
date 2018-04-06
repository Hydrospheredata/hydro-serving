package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.model.db.Environment

class AnyEnvironment extends Environment(
  AnyEnvironment.anyEnvironmentId, "Without Env", AnyEnvironment.emptyPlaceholder
)

object AnyEnvironment {
  val emptyPlaceholder = Seq()
  val anyEnvironmentId: Long = -1
}