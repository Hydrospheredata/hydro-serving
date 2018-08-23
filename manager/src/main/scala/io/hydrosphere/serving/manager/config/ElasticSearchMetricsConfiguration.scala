package io.hydrosphere.serving.manager.config

case class ElasticSearchMetricsConfiguration(
  collectTimeout: Int,
  indexName: String,
  mappingName: String,
  clientUri: String
)
