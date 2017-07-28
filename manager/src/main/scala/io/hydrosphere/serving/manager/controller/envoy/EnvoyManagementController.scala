package io.hydrosphere.serving.manager.controller.envoy

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.service.envoy._
import io.swagger.annotations._

import scala.concurrent.duration._

/**
  *
  */
@Path("/v1")
@Api(value = "/v1", produces = "application/json")
class EnvoyManagementController(envoyManagementService: EnvoyManagementService) extends EnvoyJsonSupport {
  implicit val timeout = Timeout(5.minutes)

  @Path("/clusters/{serviceId}/{containerId}")
  @ApiOperation(value = "clusters", notes = "clusters", nickname = "clusters", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceId", value = "serviceId", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "containerId", value = "containerId", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Cluster", response = classOf[EnvoyClusterConfig]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getClusters = get {
    path("v1" / "clusters" / LongNumber / Segment) { (serviceId, containerId) =>
      complete(envoyManagementService.clusters(serviceId, containerId))
    }
  }

  @Path("/routes/{configName}/{serviceId}/{containerId}")
  @ApiOperation(value = "routes", notes = "routes", nickname = "routes", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "configName", value = "configName", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "serviceId", value = "serviceId", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "containerId", value = "containerId", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "RouteConfig", response = classOf[EnvoyRouteConfig]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getRoutes = get {
    path("v1" / "routes" / Segment / LongNumber / Segment) { (configName, serviceId, containerId) =>
      complete(envoyManagementService.routes(configName, serviceId, containerId))
    }
  }

  @Path("/registration/{serviceName}")
  @ApiOperation(value = "registration", notes = "registration", nickname = "registration", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceName", value = "serviceName", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "RouteConfig", response = classOf[EnvoyServiceConfig]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getServices = get {
    path("v1" / "registration" / Segment) { serviceName =>
      complete(envoyManagementService.services(serviceName))
    }
  }


  val routes: Route = getClusters ~ getRoutes ~ getServices
}
