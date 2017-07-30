package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import io.hydrosphere.serving.manager.service.ServingManagementService
import io.hydrosphere.serving.model.Pipeline
import io.swagger.annotations._

import scala.concurrent.duration._

@Path("/api/v1/pipelines")
@Api(produces = "application/json", tags = Array("Deployment: Pipelines"))
class PipelineController(servingManagementService: ServingManagementService) extends ManagerJsonSupport {
  implicit val timeout = Timeout(5.seconds)

  @Path("/")
  @ApiOperation(value = "pipelines", notes = "pipelines", nickname = "pipelines", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Pipeline", response = classOf[Pipeline], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listAll = path("api" / "v1" / "pipelines") {
    get {
      complete(servingManagementService.allPipelines())
    }
  }

  @Path("/")
  @ApiOperation(value = "Add Pipeline", notes = "Add Pipeline", nickname = "addPipeline", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Pipeline", required = true,
      dataType = "io.hydrosphere.serving.model.Pipeline", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Pipeline", response = classOf[Pipeline]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addPipeline = path("api" / "v1" / "pipelines") {
    post {
      entity(as[Pipeline]) { r =>
        complete(
          servingManagementService.addPipeline(r)
        )
      }
    }
  }

  @Path("/{pipelineId}")
  @ApiOperation(value = "deletePipeline", notes = "deletePipeline", nickname = "deletePipeline", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "pipelineId", required = true, dataType = "long", paramType = "path", value = "pipelineId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Pipeline Deleted"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deletePipeline = delete {
    path("api" / "v1" / "pipelines" / LongNumber) { pipelineId =>
      onSuccess(servingManagementService.deletePipeline(pipelineId)) {
        complete(200, None)
      }
    }
  }

  val routes = listAll ~ addPipeline ~ deletePipeline
}
