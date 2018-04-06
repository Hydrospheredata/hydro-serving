package io.hydrosphere.serving.manager.controller.model_source

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import io.hydrosphere.serving.manager.model.db.ModelSourceConfig
import io.hydrosphere.serving.manager.service.SourceManagementService
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._
import io.swagger.annotations._


@Path("/api/v1/modelSource")
@Api(produces = "application/json", tags = Array("Model Sources"))
class ModelSourceController(sourceService: SourceManagementService) {
  @Path("/s3")
  @ApiOperation(value = "Add s3 model source", notes = "Add s3 model source", nickname = "addS3Source", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "AddS3SourceRequest", required = true,
      dataTypeClass = classOf[AddS3SourceRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelSourceConfigAux", response = classOf[ModelSourceConfig]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addS3Source = path("api" / "v1" / "modelSource" / "s3") {
    post {
      entity(as[AddS3SourceRequest]) { r =>
        complete {
          sourceService.addS3Source(r)
        }
      }
    }
  }

  @Path("/")
  @ApiOperation(value = "listModelSources", notes = "listModelSources", nickname = "listModelSources", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelSourceConfigAux", response = classOf[ModelSourceConfig], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listModelSources = path("api" / "v1" / "modelSource") {
    get {
      complete(sourceService.allSourceConfigs)
    }
  }

  val routes = addS3Source ~ listModelSources
}
