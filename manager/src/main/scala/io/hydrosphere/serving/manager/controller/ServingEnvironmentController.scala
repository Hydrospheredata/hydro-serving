package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.model.ServingEnvironment
import io.hydrosphere.serving.manager.service._
import io.swagger.annotations._

import scala.concurrent.duration._

/**
  *
  */
@Path("/api/v1/servingEnvironment")
@Api(produces = "application/json", tags = Array("Deployment: Serving Environment"))
class ServingEnvironmentController(
  runtimeManagementService: RuntimeManagementService
) extends ManagerJsonSupport {
  implicit val timeout = Timeout(5.seconds)

  @Path("/")
  @ApiOperation(value = "listServingEnvironment", notes = "listServingEnvironment", nickname = "listServingEnvironment", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Serving Environments", response = classOf[ServingEnvironment], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def list = path("api" / "v1" / "servingEnvironment") {
    get {
      complete(runtimeManagementService.allServingEnvironments())
    }
  }

  @Path("/")
  @ApiOperation(value = "Create ServingEnvironment", notes = "Create ServingEnvironment", nickname = "createServingEnvironment", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "ServingEnvironment Object", required = true,
      dataType = "io.hydrosphere.serving.manager.service.CreateServingEnvironment", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ServingEnvironment", response = classOf[ServingEnvironment]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def create = path("api" / "v1" / "servingEnvironment") {
    entity(as[CreateServingEnvironment]) { r =>
      complete(
        runtimeManagementService.createServingEnvironment(r)
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
    path("api" / "v1" / "servingEnvironment" / LongNumber) { environmentId =>
      onSuccess(runtimeManagementService.deleteServingEnvironment(environmentId)) {
        complete(200, None)
      }
    }
  }

  val routes: Route = list ~ create ~ deleteEnvironment
}
