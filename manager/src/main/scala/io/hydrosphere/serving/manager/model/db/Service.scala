package io.hydrosphere.serving.manager.model.db

case class Service(
  id: Long,
  serviceName: String,
  cloudDriverId: Option[String],
  runtime: Runtime,
  model: Option[ModelVersion],
  environment: Option[Environment],
  statusText: String,
  configParams: Map[String, String]
) {
  def toServiceKeyDescription: ServiceKeyDescription =
    ServiceKeyDescription(
      runtimeId = runtime.id,
      modelVersionId = model.map(_.id),
      environmentId = environment.map(_.id)
    )
}
