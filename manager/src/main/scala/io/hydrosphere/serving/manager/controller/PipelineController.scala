package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import io.hydrosphere.serving.controller.TracingHeaders
import io.hydrosphere.serving.manager.service.{CreatePipelineRequest, PipelineKey, ServeRequest, ServingManagementService}
import io.hydrosphere.serving.model.Pipeline
import io.swagger.annotations._

import scala.concurrent.duration._

@Path("/api/v1/pipelines")
@Api(produces = "application/json", tags = Array("Deployment: Pipelines"))
class PipelineController(servingManagementService: ServingManagementService)
  extends ManagerJsonSupport
  with ServingDataDirectives {

  implicit val timeout = Timeout(5.minutes)

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
      dataTypeClass = classOf[CreatePipelineRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Pipeline", response = classOf[Pipeline]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addPipeline = path("api" / "v1" / "pipelines") {
    post {
      entity(as[CreatePipelineRequest]) { r =>
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

  @Path("/serve/{pipelineId}")
  @ApiOperation(value = "Serve Pipeline", notes = "Serve Pipeline", nickname = "ServePipeline", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "pipelineId", required = true, dataType = "long", paramType = "path", value = "pipelineId"),
    new ApiImplicitParam(name = "body", value = "Any",dataTypeClass = classOf[List[_]], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def servePipeline = path("api" / "v1" / "pipelines" / "serve" / LongNumber) { pipelineId =>
    post {
      extractRequest { request =>
        extractRawData { bytes =>
          val serveRequest = ServeRequest(
            serviceKey = PipelineKey(pipelineId),
            servePath = "/serve",
            headers = request.headers.filter(h => TracingHeaders.isTracingHeaderName(h.name())),
            inputData = bytes
          )
          completeExecutionResult(servingManagementService.serve(serveRequest))
        }
      }
    }
  }

  val routes = listAll ~ addPipeline ~ deletePipeline ~ servePipeline
}
