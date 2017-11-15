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
@Api(produces = "application/json", tags = Array("Infrastructure: Envoy"))
class EnvoyManagementController(envoyManagementService: EnvoyManagementService) extends EnvoyJsonSupport {
  implicit val timeout = Timeout(5.minutes)

  @Path("/clusters/{fullName}/{containerId}")
  @ApiOperation(value = "clusters", notes = "clusters", nickname = "clusters", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "fullName", value = "fullName", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "containerId", value = "containerId", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Cluster", response = classOf[EnvoyClusterConfig]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getClusters = get {
    path("v1" / "clusters" / Segment / Segment) { (fullName, containerId) =>
      complete(envoyManagementService.clusters(fullName, containerId))
    }
  }

  @Path("/routes/{configName}/{fullName}/{containerId}")
  @ApiOperation(value = "routes", notes = "routes", nickname = "routes", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "configName", value = "configName", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "fullName", value = "fullName", required = true, dataType = "string", paramType = "path"),
    new ApiImplicitParam(name = "containerId", value = "containerId", required = true, dataType = "string", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "RouteConfig", response = classOf[EnvoyRouteConfig]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getRoutes = get {
    path("v1" / "routes" / Segment / Segment / Segment) { (configName, fullName, containerId) =>
      complete(envoyManagementService.routes(configName, fullName, containerId))
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
