package io.hydrosphere.serving.manager.service.runtime

case class CreateRuntimeRequest(
  name: String,
  version: String,
  modelTypes: List[String] = List.empty,
  tags: List[String] = List.empty,
  configParams: Map[String, String] = Map.empty
) {
  def fullImage = name + ":" + version
}

case class SyncRuntimeRequest(
  runtimeId: Long
)

case class PullDocker(
  image: String,
  version: String
) {
  def fullImage = image + ":" + version
}