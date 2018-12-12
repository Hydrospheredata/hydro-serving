package io.hydrosphere.serving.manager.domain.clouddriver

case class MetricServiceTargets(
  targets: List[String],
  labels: MetricServiceTargetLabels
)
