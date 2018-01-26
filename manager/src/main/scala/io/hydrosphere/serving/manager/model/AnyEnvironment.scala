package io.hydrosphere.serving.manager.model

class AnyEnvironment extends Environment(
  AnyEnvironment.anyEnvironmentId, "Without Env", AnyEnvironment.emptyPlaceholder
)

object AnyEnvironment {
  val emptyPlaceholder = Seq()
  val anyEnvironmentId: Long = -1
}