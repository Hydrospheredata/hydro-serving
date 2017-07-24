package io.hydrosphere.serving.manager.service.envoy

import scala.concurrent.Future


case class EnvoyClusterHost(
  url: String
)

case class EnvoyCluster(
  name: String,
  `type`: String,
  connect_timeout_ms: Long,
  lb_type: String,
  hosts: Seq[EnvoyClusterHost],
  service_name: String,
  features: String
)

case class EnvoyClusterConfig(
  clusters: Seq[EnvoyCluster]
)

case class EnvoyRoute(
  prefix: String,
  cluster: String
)

case class EnvoyRouteHost(
  name: String,
  domains: Seq[String],
  routes: Seq[EnvoyRoute]
)

case class EnvoyRouteConfig(
  virtual_hosts: Seq[EnvoyRouteHost]
)

case class EnvoyServiceTags(
  az: String,
  canary: String,
  load_balancing_weight: String
)

case class EnvoyServiceHost(
  ip_address: String,
  port: Int,
  tags: Seq[EnvoyServiceTags]
)

case class EnvoyServiceConfig(
  hosts: Seq[EnvoyServiceHost]
)

trait EnvoyManagementService {
  def clusters(cluster: String, node: String): Future[EnvoyClusterConfig]

  def services(serviceName: String): Future[EnvoyServiceConfig]

  def routes(configName: String, cluster: String, node: String): Future[EnvoyRouteConfig]
}
