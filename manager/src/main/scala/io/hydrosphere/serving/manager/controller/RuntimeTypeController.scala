package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.model.RuntimeType
import io.hydrosphere.serving.manager.service.{CreateRuntimeTypeRequest, ModelManagementService, RuntimeTypeManagementService}
import io.hydrosphere.serving.model_api.ModelType
import io.swagger.annotations._

import scala.concurrent.duration._

/**
  *
  */
@Path("/api/v1/runtimeType")
@Api(produces = "application/json", tags = Array("Models: RuntimeType"))
class RuntimeTypeController(modelManagementService: ModelManagementService, runtimeTypeManagementService: RuntimeTypeManagementService) extends ManagerJsonSupport {
  implicit val timeout = Timeout(5.seconds)

  @Path("/")
  @ApiOperation(value = "listRuntimeType", notes = "listRuntimeType", nickname = "listRuntimeType", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Runtime Types", response = classOf[RuntimeType], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listRuntimeType = path("api" / "v1" / "runtimeType") {
    get {
      println("123KEK123")

      complete(modelManagementService.allRuntimeTypes())
    }
  }

  @Path("/")
  @ApiOperation(value = "Create Runtime Type", notes = "Create Runtime Type", nickname = "createRuntimeType", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Runtime Type Object", required = true,
      dataType = "io.hydrosphere.serving.manager.service.CreateRuntimeTypeRequest", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Runtime", response = classOf[RuntimeType]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def createRuntime = path("api" / "v1" / "runtimeType") {
    entity(as[CreateRuntimeTypeRequest]) { r =>
      complete(
        modelManagementService.createRuntimeType(r)
      )
    }
  }

  @Path("/modelType/{modelType}")
  @ApiOperation(value = "Lookup by a modelType", notes = "Lookup by a modelType", nickname = "lookupByModelType", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelType", required = true, dataType = "string", paramType = "path", value = "tag")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Runtime", response = classOf[Seq[RuntimeType]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def lookupByTag = path("api" / "v1" / "runtimeType" / "modelType" / Segment) { tag =>
    get {
      complete {
        ModelType.tryFromTag(tag).map(runtimeTypeManagementService.lookupRuntimeType)
      }
    }
  }

  val routes: Route = listRuntimeType ~ createRuntime ~ lookupByTag
}