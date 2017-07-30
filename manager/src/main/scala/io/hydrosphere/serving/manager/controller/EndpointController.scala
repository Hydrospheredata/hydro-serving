package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import io.hydrosphere.serving.manager.service.ServingManagementService
import io.hydrosphere.serving.model.Endpoint
import io.swagger.annotations._
import io.swagger.annotations.Api

import scala.concurrent.duration._

/**
  *
  */
@Path("/api/v1/endpoints")
@Api(produces = "application/json", tags = Array("Deployment: Endpoints"))
class EndpointController(servingManagementService: ServingManagementService) extends ManagerJsonSupport {
  implicit val timeout = Timeout(5.seconds)

  @Path("/")
  @ApiOperation(value = "endpoints", notes = "endpoints", nickname = "endpoints", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Endpoint", response = classOf[Endpoint], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listAll = path("api" / "v1" / "endpoints") {
    get {
      complete(servingManagementService.allEndpoints())
    }
  }


  @Path("/")
  @ApiOperation(value = "Add Endpoint", notes = "Add Endpoint", nickname = "addEndpoint", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Endpoint", required = true,
      dataType = "io.hydrosphere.serving.model.Endpoint", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Endpoint", response = classOf[Endpoint]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def add = path("api" / "v1" / "endpoints") {
    post {
      entity(as[Endpoint]) { r =>
        complete(
          servingManagementService.addEndpoint(r)
        )
      }
    }
  }

  @Path("/{endpointId}")
  @ApiOperation(value = "deleteEndpoint", notes = "deleteEndpoint", nickname = "deleteEndpoint", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "endpointId", required = true, dataType = "long", paramType = "path", value = "endpointId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Endpoint Deleted"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deleteEndpoint = delete {
    path("api" / "v1" / "endpoints" / LongNumber) { endpointId =>
      onSuccess(servingManagementService.deleteEndpoint(endpointId)) {
        complete(200, None)
      }
    }
  }

  val routes = listAll ~ add ~ deleteEndpoint

}
