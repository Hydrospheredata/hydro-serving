package io.hydrosphere.serving.manager.domain.clouddriver

case class MetricServiceTargetLabels(
  job: Option[String],
  modelName: Option[String],
  modelVersion: Option[String],
  environment: Option[String],
  runtimeName: Option[String],
  runtimeVersion: Option[String],
  serviceName: Option[String],
  serviceId: Option[String],
  serviceCloudDriverId: Option[String],
  serviceType: Option[String],
  instanceId: Option[String]
)
