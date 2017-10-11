//package io.hydrosphere.serving.manager.controller
//
//import javax.ws.rs.Path
//
//import akka.http.scaladsl.server.Directives._
//import akka.util.Timeout
//import io.hydrosphere.serving.controller.TracingHeaders
//import io.hydrosphere.serving.manager.service.{CreatePipelineRequest, ServingManagementService, SourceManagementService}
//import io.hydrosphere.serving.model.Pipeline
//import io.swagger.annotations._
//
//import scala.concurrent.duration._
//
//@Path("/api/v1/modelSource")
//@Api(produces = "application/json", tags = Array("Deployment: Model Sources"))
//class ModelSourceController(implicit sourceManagementService: SourceManagementService) extends ManagerJsonSupport {
//  implicit val timeout = Timeout(5.minutes)
//
//  @Path("/")
//  @ApiOperation(value = "sources", notes = "sources", nickname = "sources", httpMethod = "GET")
//  @ApiResponses(Array(
//    new ApiResponse(code = 200, message = "ModelSource", response = classOf[Pipeline], responseContainer = "List"),
//    new ApiResponse(code = 500, message = "Internal server error")
//  ))
//  def listAll = path("api" / "v1" / "modelSources") {
//    get {
//      complete(sourceManagementService.getSources)
//    }
//  }
//
//  @Path("/")
//  @ApiOperation(value = "Add Pipeline", notes = "Add Pipeline", nickname = "addPipeline", httpMethod = "POST")
//  @ApiImplicitParams(Array(
//    new ApiImplicitParam(name = "body", value = "Pipeline", required = true,
//      dataTypeClass = classOf[CreatePipelineRequest], paramType = "body")
//  ))
//  @ApiResponses(Array(
//    new ApiResponse(code = 200, message = "Pipeline", response = classOf[Pipeline]),
//    new ApiResponse(code = 500, message = "Internal server error")
//  ))
//  def addPipeline = path("api" / "v1" / "pipelines") {
//    post {
//      entity(as[CreatePipelineRequest]) { r =>
//        complete(
//          servingManagementService.addPipeline(r)
//        )
//      }
//    }
//  }
//
//  @Path("/{pipelineId}")
//  @ApiOperation(value = "deletePipeline", notes = "deletePipeline", nickname = "deletePipeline", httpMethod = "DELETE")
//  @ApiImplicitParams(Array(
//    new ApiImplicitParam(name = "pipelineId", required = true, dataType = "long", paramType = "path", value = "pipelineId")
//  ))
//  @ApiResponses(Array(
//    new ApiResponse(code = 200, message = "Pipeline Deleted"),
//    new ApiResponse(code = 500, message = "Internal server error")
//  ))
//  def deletePipeline = delete {
//    path("api" / "v1" / "pipelines" / LongNumber) { pipelineId =>
//      onSuccess(servingManagementService.deletePipeline(pipelineId)) {
//        complete(200, None)
//      }
//    }
//  }
//
//  @Path("/serve/{pipelineId}")
//  @ApiOperation(value = "Serve Pipeline", notes = "Serve Pipeline", nickname = "ServePipeline", httpMethod = "POST")
//  @ApiImplicitParams(Array(
//    new ApiImplicitParam(name = "pipelineId", required = true, dataType = "long", paramType = "path", value = "pipelineId"),
//    new ApiImplicitParam(name = "body", value = "Any",dataTypeClass = classOf[List[_]], required = true, paramType = "body")
//  ))
//  @ApiResponses(Array(
//    new ApiResponse(code = 200, message = "Any"),
//    new ApiResponse(code = 500, message = "Internal server error")
//  ))
//  def servePipeline = path("api" / "v1" / "pipelines" / "serve" / LongNumber) { pipelineId =>
//    post {
//      extractRequest { request =>
//        entity(as[Seq[Any]]) { r =>
//          complete(
//            servingManagementService.servePipeline(pipelineId, r, request.headers
//              .filter(h => TracingHeaders.isTracingHeaderName(h.name())))
//          )
//        }
//      }
//    }
//  }
//
//  val routes = listAll ~ addPipeline ~ deletePipeline ~ servePipeline
//}
