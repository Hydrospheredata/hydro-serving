package io.hydrosphere.serving.manager.service.prometheus

import scala.concurrent.Future


trait PrometheusMetricsService {
  def fetchServices(): Future[Seq[ServiceTargets]]

  def fetchMetrics(serviceId: Long, instanceId: String): Future[String]
}
