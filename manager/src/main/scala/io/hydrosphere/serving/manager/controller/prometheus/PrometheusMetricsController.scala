package io.hydrosphere.serving.manager.controller.prometheus

import javax.ws.rs.Path

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.service.prometheus.PrometheusMetricsService
import io.hydrosphere.serving.manager.model.CommonJsonSupport._
import io.hydrosphere.serving.manager.service.clouddriver.MetricServiceTargets
import io.swagger.annotations._

import scala.concurrent.duration._

@Path("/v1/prometheus")
@Api(produces = "application/json", tags = Array("Infrastructure: Prometheus"))
class PrometheusMetricsController(
  prometheusMetricsService:PrometheusMetricsService
) {
  implicit val timeout = Timeout(5.minutes)

  @Path("/services")
  @ApiOperation(value = "services", notes = "services", nickname = "services", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "services", response = classOf[MetricServiceTargets], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getServices = get {
    path("v1" / "prometheus" / "services") {
      complete(prometheusMetricsService.fetchServices())
    }
  }

  @Path("/proxyMetrics/{serviceId}/{instanceId}/{serviceType}")
  @ApiOperation(value = "proxyMetrics", notes = "proxyMetrics", nickname = "proxyMetrics", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceId", value = "serviceId", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "instanceId", value = "instanceId", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "serviceType", value = "serviceType", required = true, dataType = "string", paramType = "path")

  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "metrics"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def proxyMetrics = get {
    path("v1" / "prometheus" / "proxyMetrics" / Segment / Segment /Segment ) { (serviceId, instanceId, serviceType) =>
      extractRequest { request => {
        complete(prometheusMetricsService.fetchMetrics(serviceId.toLong, instanceId, serviceType))
      }
      }
    }
  }

  val routes: Route = getServices ~ proxyMetrics

}
