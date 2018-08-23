package io.hydrosphere.serving.manager.config

case class MetricsConfiguration(
  elasticSearch: Option[ElasticSearchMetricsConfiguration],
  influxDb: Option[InfluxDBMetricsConfiguration]
)