package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.RuntimeType
import io.hydrosphere.serving.manager.service.ModelManagementService
import io.swagger.annotations._

import scala.concurrent.duration._

/**
  *
  */
@Path("/api/v1/runtimeType")
@Api(produces = "application/json", tags = Array("Models: RuntimeType"))
class RuntimeTypeController(modelManagementService: ModelManagementService) extends ManagerJsonSupport {
  implicit val timeout = Timeout(5.seconds)

  @Path("/")
  @ApiOperation(value = "listRuntimeType", notes = "listRuntimeType", nickname = "listRuntimeType", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Runtime Types", response = classOf[RuntimeType], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listRuntimeType = path("api" / "v1" / "runtimeType") {
    get {
      complete(modelManagementService.allRuntimeTypes())
    }
  }

  @Path("/")
  @ApiOperation(value = "Create Runtime Type", notes = "Create Runtime Type", nickname = "createRuntimeType", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Runtime Type Object", required = true,
      dataType = "io.hydrosphere.serving.manager.model.RuntimeType", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Runtime", response = classOf[RuntimeType]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def createRuntime = path("api" / "v1" / "runtimeType") {
    entity(as[RuntimeType]) { r =>
      complete(
        modelManagementService.createRuntimeType(r)
      )
    }
  }

  val routes: Route = listRuntimeType ~ createRuntime
}