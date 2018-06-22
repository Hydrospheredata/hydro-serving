package io.hydrosphere.serving.manager.controller.runtime

import java.util.UUID

import javax.ws.rs.Path
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.controller.GenericController
import io.hydrosphere.serving.manager.model.db.Runtime
import io.hydrosphere.serving.manager.service.{ServiceTask, ServiceTaskFailed, ServiceTaskFinished, ServiceTaskRunning}
import io.hydrosphere.serving.manager.service.runtime.{CreateRuntimeRequest, RuntimeManagementService}
import io.hydrosphere.serving.manager.util.UUIDUtils
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

  @Path("/status/{requestId}")
  @ApiOperation(value = "Get creation status", notes = "Returns runtime status", nickname = "getStatus", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "requestId", value = "Request UUID", required = true, dataTypeClass = classOf[UUID], paramType = "path", defaultValue = UUIDUtils.zerosStr)
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "TaskFinished", response = classOf[ServiceTaskFinished[CreateRuntimeRequest, Runtime]]),
    new ApiResponse(code = 200, message = "TaskRunning", response = classOf[ServiceTaskRunning[CreateRuntimeRequest, Runtime]]),
    new ApiResponse(code = 200, message = "TaskFailed", response = classOf[ServiceTaskFailed[CreateRuntimeRequest, Runtime]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getStatus = path("api" / "v1" / "runtime" / "status" / JavaUUID) { uuid =>
    get {
      completeFRes(runtimeManagementService.getCreationStatus(uuid))
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
          runtimeManagementService.create(r)
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

  val routes: Route = listRuntime ~ createRuntime ~ lookupRuntimeByTag ~ getStatus
}