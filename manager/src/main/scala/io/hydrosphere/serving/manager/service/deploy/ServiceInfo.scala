package io.hydrosphere.serving.manager.service.deploy

case class ServiceInfo(
  id: Long,
  name: String,
  cloudDriveId: String,
  status: String,
  statusText: String,
  configParams: Map[String, String]
)
