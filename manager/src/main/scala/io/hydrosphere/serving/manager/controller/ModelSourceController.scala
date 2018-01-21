package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._

import io.hydrosphere.serving.manager.model.ModelSourceConfigAux
import io.hydrosphere.serving.manager.service.{CreateModelSourceRequest, SourceManagementService}
import io.swagger.annotations._

@Path("/api/v1/modelSource")
@Api(produces = "application/json", tags = Array("Model Sources"))
class ModelSourceController(sourceService: SourceManagementService) extends ManagerJsonSupport {
  @Path("/")
  @ApiOperation(value = "Add model source", notes = "Add model source", nickname = "addSource", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "CreateModelSourceRequest", required = true,
      dataTypeClass = classOf[CreateModelSourceRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelSourceConfigAux", response = classOf[ModelSourceConfigAux]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addModelSource = path("api" / "v1" / "modelSource") {
    post {
      entity(as[CreateModelSourceRequest]) { r =>
        complete {
          sourceService.addSource(r)
          Map("msg" -> "ModelSource added. Soon watcher will fetch models and display them.")
        }
      }
    }
  }

  @Path("/")
  @ApiOperation(value = "listModelSources", notes = "listModelSources", nickname = "listModelSources", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelSourceConfigAux", response = classOf[ModelSourceConfigAux], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listModelSources = path("api" / "v1" / "modelSource") {
    get {
      complete(sourceService.getSourceConfigs)
    }
  }

  val routes = addModelSource ~ listModelSources
}
