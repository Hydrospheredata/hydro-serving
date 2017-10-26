package io.hydrosphere.serving.manager.service.envoy

import java.util.UUID

import io.hydrosphere.serving.manager.model.ModelServiceInstance
import io.hydrosphere.serving.manager.service.{RuntimeManagementService, ServingManagementService}
import io.hydrosphere.serving.model.ModelService
import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}


case class EnvoyClusterHost(
  url: String
)

case class EnvoyCluster(
  name: String,
  `type`: String,
  connect_timeout_ms: Long,
  lb_type: String,
  hosts: Option[Seq[EnvoyClusterHost]],
  service_name: Option[String],
  features: Option[String]
)

case class EnvoyClusterConfig(
  clusters: Seq[EnvoyCluster]
)

case class EnvoyRouteWeightedCluster(
  name: String,
  weight: Int
)

case class EnvoyRouteWeightedClusters(
  clusters: Seq[EnvoyRouteWeightedCluster]
)

case class EnvoyRoute(
  prefix: String,
  cluster: Option[String],
  weighted_clusters: Option[EnvoyRouteWeightedClusters]
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
  tags: Option[Seq[EnvoyServiceTags]]
)

case class EnvoyServiceConfig(
  hosts: Seq[EnvoyServiceHost]
)

trait EnvoyManagementService {
  def clusters(serviceId: Long, containerId: String): Future[EnvoyClusterConfig]

  def services(serviceName: String): Future[EnvoyServiceConfig]

  def routes(configName: String, serviceId: Long, containerId: String): Future[EnvoyRouteConfig]
}

class EnvoyManagementServiceImpl(
  runtimeManagementService: RuntimeManagementService,
  servingManagementService: ServingManagementService
)(implicit val ex: ExecutionContext) extends EnvoyManagementService with Logging {

  private def fetchGatewayIfNeeded(modelService: ModelService): Future[Seq[ModelServiceInstance]] = {
    if (modelService.serviceId >= 0) {
      runtimeManagementService.instancesForService(runtimeManagementService.GATEWAY_ID)
    } else {
      Future.successful(Seq())
    }
  }

  override def routes(configName: String, serviceId: Long, containerId: String): Future[EnvoyRouteConfig] = {
    runtimeManagementService.getService(serviceId).flatMap(servOp => {
      runtimeManagementService.allServices().flatMap(services => {
        servingManagementService.allWeightedServices().flatMap(wightedServices => {
          val modelService = servOp.get
          fetchGatewayIfNeeded(modelService).map(gatewayServiceInstances => {

            val routeHosts = mutable.MutableList[EnvoyRouteHost]()

            wightedServices.foreach(s => {
              val weights=Some(EnvoyRouteWeightedClusters(
                s.weights.map(w => EnvoyRouteWeightedCluster(
                  //TODO optimize search
                  name = services.find(f => f.serviceId == w.serviceId).get.serviceName,
                  weight = w.weight
                ))
              ))

              routeHosts += EnvoyRouteHost(
                name = s.serviceName.toLowerCase,
                domains = Seq(s.serviceName.toLowerCase),
                routes = Seq(EnvoyRoute(
                  prefix = "/",
                  cluster = None,
                  weighted_clusters = weights))
              )
              if(s.sourcesList.nonEmpty){
                routeHosts += EnvoyRouteHost(
                  name = s"weightedservices${s.id}",
                  domains = Seq(s"weightedservices${s.id}"),
                  routes = Seq(EnvoyRoute(
                    prefix = "/",
                    cluster = None,
                    weighted_clusters = weights))
                )
              }
            })

            services.filter(s => s.serviceId != serviceId)
              .foreach(s => {
                routeHosts += EnvoyRouteHost(
                  name = s.serviceName.toLowerCase,
                  domains = Seq(s.serviceName.toLowerCase),
                  routes = Seq(EnvoyRoute("/", Some(s.serviceName), None))
                )
              })

            gatewayServiceInstances.foreach(s => {
              routeHosts += EnvoyRouteHost(
                name = s.instanceId.toLowerCase,
                domains = Seq(s.instanceId.toLowerCase),
                routes = Seq(EnvoyRoute("/", Some(UUID.nameUUIDFromBytes(s.instanceId.getBytes()).toString), None))
              )
            })

            routeHosts += EnvoyRouteHost(
              name = "all",
              domains = Seq("*"),
              routes = Seq(EnvoyRoute("/", Some(modelService.serviceName), None))
            )

            EnvoyRouteConfig(
              virtual_hosts = routeHosts
            )
          })
        })
      })
    })
  }

  override def services(serviceName: String): Future[EnvoyServiceConfig] =
    runtimeManagementService.instancesForService(serviceName)
      .map(seq => {
        EnvoyServiceConfig(
          hosts = seq.map(s =>
            EnvoyServiceHost(
              ip_address = s.host,
              port = s.sidecarPort,
              tags = None
            )
          )
        )
      })

  override def clusters(serviceId: Long, containerId: String): Future[EnvoyClusterConfig] = {
    runtimeManagementService.getService(serviceId).flatMap(servOp => {
      runtimeManagementService.instancesForService(serviceId).flatMap(instancesSame => {
        runtimeManagementService.allServices().flatMap(services => {
          val modelService = servOp.get
          val containerInstance = instancesSame.find(p => p.serviceId == serviceId)
          if (containerInstance.isEmpty) {
            Future.successful(EnvoyClusterConfig(Seq()))
          } else {
            fetchGatewayIfNeeded(modelService).map(gatewayServiceInstances => {
              val clustres = mutable.MutableList[EnvoyCluster]()

              services.foreach(s => {
                if (s.serviceId == modelService.serviceId) {
                  clustres += EnvoyCluster(
                    features = None,
                    connect_timeout_ms = 500,
                    lb_type = "round_robin",
                    service_name = None,
                    name = s.serviceName,
                    `type` = "static",
                    hosts = Some(Seq(EnvoyClusterHost(s"tcp://127.0.0.1:${containerInstance.get.appPort}")))
                  )
                } else {
                  clustres += EnvoyCluster(
                    features = None,
                    connect_timeout_ms = 500,
                    lb_type = "round_robin",
                    service_name = Some(s.serviceName),
                    name = s.serviceName,
                    `type` = "sds",
                    hosts = None
                  )
                }
              })
              gatewayServiceInstances.foreach(s => {
                clustres += EnvoyCluster(
                  features = None,
                  connect_timeout_ms = 500,
                  lb_type = "round_robin",
                  service_name = None,
                  name = UUID.nameUUIDFromBytes(s.instanceId.getBytes).toString,
                  `type` = "static",
                  hosts = Some(getStaticHost(modelService, s, containerId))
                )
              })

              EnvoyClusterConfig(
                clusters = clustres
              )
            })
          }
        })
      })
    })
  }


  private def getStaticHost(runtime: ModelService, service: ModelServiceInstance, forNode: String): Seq[EnvoyClusterHost] = {
    val sameNode = service.instanceId == forNode
    val builder = new StringBuilder("tcp://")
    if (sameNode)
      builder.append("127.0.0.1")
    else
      builder.append(service.host)
    builder.append(":")
    if (sameNode)
      builder.append(service.appPort)
    else
      builder.append(service.sidecarPort)
    Seq(EnvoyClusterHost(builder.toString))
  }

}
