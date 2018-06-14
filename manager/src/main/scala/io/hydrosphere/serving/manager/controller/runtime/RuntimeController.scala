package io.hydrosphere.serving.manager.controller.runtime

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.controller.GenericController
import io.hydrosphere.serving.manager.model.db.Runtime
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.runtime.RuntimeManagementService
import io.swagger.annotations._

import scala.concurrent.duration._

@Path("/api/v1/runtime")
@Api(produces = "application/json", tags = Array("Runtime"))
class RuntimeController(
  runtimeManagementService: RuntimeManagementService
) extends GenericController {
  implicit val timeout = Timeout(5.seconds)

  @Path("/")
  @ApiOperation(value = "listRuntime", notes = "listRuntime", nickname = "listRuntime", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Runtimes", response = classOf[Runtime], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listRuntime = path("api" / "v1" / "runtime") {
    get {
      completeF(runtimeManagementService.all())
    }
  }

  @Path("/")
  @ApiOperation(value = "Create Runtime", notes = "Create Runtime", nickname = "createRuntime", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Runtime Object", required = true, dataTypeClass = classOf[CreateRuntimeRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Runtime", response = classOf[Runtime]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def createRuntime = path("api" / "v1" / "runtime") {
    post{
      entity(as[CreateRuntimeRequest]) { r =>
        completeF(
          runtimeManagementService.create(
            r.name,
            r.version,
            r.modelTypes.getOrElse(List.empty),
            r.tags.getOrElse(List.empty),
            r.configParams.getOrElse(Map.empty)
          )
        )
      }
    }
  }

  @Path("/modelType/{modelType}")
  @ApiOperation(value = "Lookup by a modelType", notes = "Lookup by a modelType", nickname = "lookupByModelType", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelType", required = true, dataType = "string", paramType = "path", value = "tag")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Runtime", response = classOf[Seq[Runtime]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def lookupRuntimeByTag = path("api" / "v1" / "runtime" / "modelType" / Segment) { tag =>
    get {
      completeF {
        runtimeManagementService.lookupByModelType(Set(tag))
      }
    }
  }

  val routes: Route = listRuntime ~ createRuntime ~ lookupRuntimeByTag
}