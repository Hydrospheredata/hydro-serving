package io.hydrosphere.serving.manager.model

import io.hydrosphere.serving.manager.util.CommonJsonSupport._

case class Environment(
  id: Long,
  name: String,
  placeholders: Seq[Any]
)

object Environment {
  implicit val environmentFormat = jsonFormat3(Environment.apply)
}
