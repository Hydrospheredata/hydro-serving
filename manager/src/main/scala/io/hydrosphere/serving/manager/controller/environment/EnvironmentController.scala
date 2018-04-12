package io.hydrosphere.serving.manager.controller.environment

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.controller.GenericController
import io.hydrosphere.serving.manager.model.db.Environment
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.environment.EnvironmentManagementService
import io.swagger.annotations._

import scala.concurrent.duration._


@Path("/api/v1/environment")
@Api(produces = "application/json", tags = Array("Environment"))
class EnvironmentController(
  environmentManagementService: EnvironmentManagementService
) extends GenericController {
  implicit val timeout = Timeout(5.seconds)

  @Path("/")
  @ApiOperation(value = "listEnvironment", notes = "listEnvironment", nickname = "listEnvironment", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Serving Environments", response = classOf[Environment], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listEnvironment = path("api" / "v1" / "environment") {
    get {
      complete(environmentManagementService.all())
    }
  }

  @Path("/")
  @ApiOperation(value = "Create Environment", notes = "Create Environment", nickname = "createEnvironment", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Environment Object", required = true,
      dataTypeClass = classOf[CreateEnvironmentRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Environment", response = classOf[Environment]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def createEnvironment = path("api" / "v1" / "environment") {
    entity(as[CreateEnvironmentRequest]) { r =>
      complete(
        environmentManagementService.create(r.name, r.placeholders)
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
      onSuccess(environmentManagementService.delete(environmentId)) {
        complete(200, None)
      }
    }
  }

  val routes: Route = listEnvironment ~ createEnvironment ~ deleteEnvironment
}
