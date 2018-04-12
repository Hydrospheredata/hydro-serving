package io.hydrosphere.serving.manager.controller.runtime

case class CreateRuntimeRequest(
  name: String,
  version: String,
  modelTypes: Option[List[String]] = None,
  tags: Option[List[String]] = None,
  configParams: Option[Map[String, String]] = None
)