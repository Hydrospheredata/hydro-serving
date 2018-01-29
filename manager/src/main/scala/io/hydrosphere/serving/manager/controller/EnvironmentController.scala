package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.{Environment, ManagerJsonSupport}
import io.hydrosphere.serving.manager.service._
import io.swagger.annotations._

import scala.concurrent.duration._

/**
  *
  */
@Path("/api/v1/environment")
@Api(produces = "application/json", tags = Array("Environment"))
class EnvironmentController(
  serviceManagementService: ServiceManagementService
) extends ManagerJsonSupport {
  implicit val timeout = Timeout(5.seconds)

  @Path("/")
  @ApiOperation(value = "listEnvironment", notes = "listEnvironment", nickname = "listEnvironment", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Serving Environments", response = classOf[Environment], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listEnvironment = path("api" / "v1" / "environment") {
    get {
      complete(serviceManagementService.allEnvironments())
    }
  }

  @Path("/")
  @ApiOperation(value = "Create Environment", notes = "Create Environment", nickname = "createEnvironment", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Environment Object", required = true,
      dataType = "io.hydrosphere.serving.manager.service.CreateEnvironmentRequest", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Environment", response = classOf[Environment]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def createEnvironment = path("api" / "v1" / "environment") {
    entity(as[CreateEnvironmentRequest]) { r =>
      complete(
        serviceManagementService.createEnvironment(r)
      )
    }
  }

  @Path("/{environmentId}")
  @ApiOperation(value = "deleteEnvironment", notes = "deleteEnvironment", nickname = "deleteEnvironment", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "environmentId", required = true, dataType = "long", paramType = "path", value = "environmentId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Environment Deleted"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deleteEnvironment = delete {
    path("api" / "v1" / "environment" / LongNumber) { environmentId =>
      onSuccess(serviceManagementService.deleteEnvironment(environmentId)) {
        complete(200, None)
      }
    }
  }

  val routes: Route = listEnvironment ~ createEnvironment ~ deleteEnvironment
}
