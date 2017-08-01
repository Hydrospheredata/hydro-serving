package io.hydrosphere.serving.manager.controller.envoy

import io.hydrosphere.serving.manager.controller.ManagerJsonSupport
import io.hydrosphere.serving.manager.service.envoy._

/**
  *
  */
trait EnvoyJsonSupport extends ManagerJsonSupport {

  implicit val envoyClusterHostFormat = jsonFormat1(EnvoyClusterHost)
  implicit val envoyClusterFormat = jsonFormat7(EnvoyCluster)
  implicit val envoyClusterConfigFormat = jsonFormat1(EnvoyClusterConfig)

  implicit val envoyRouteFormat = jsonFormat2(EnvoyRoute)
  implicit val envoyRouteHostFormat = jsonFormat3(EnvoyRouteHost)
  implicit val envoyRouteConfigFormat = jsonFormat1(EnvoyRouteConfig)

  implicit val envoyServiceTagsFormat = jsonFormat3(EnvoyServiceTags)
  implicit val envoyServiceHostFormat = jsonFormat3(EnvoyServiceHost)
  implicit val envoyServiceConfigFormat = jsonFormat1(EnvoyServiceConfig)
}
