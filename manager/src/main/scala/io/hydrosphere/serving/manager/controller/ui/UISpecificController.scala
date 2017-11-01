package io.hydrosphere.serving.manager.controller.ui

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.util.Timeout
import io.hydrosphere.serving.controller.TracingHeaders
import io.hydrosphere.serving.manager.controller.{BuildModelRequest, ServingDataDirectives}
import io.hydrosphere.serving.manager.service.{ModelInfo, UIManagementService}
import io.swagger.annotations._

import scala.concurrent.duration._

@Path("/ui/v1/model")
@Api(produces = "application/json", tags = Array("UI: Model"))
class UISpecificController(
  uiManagementService: UIManagementService
) extends UIJsonSupport with ServingDataDirectives {
  implicit val timeout = Timeout(5.seconds)

  @Path("/withInfo")
  @ApiOperation(value = "models", notes = "models", nickname = "models", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelInfo", response = classOf[ModelInfo], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def modelsWithLastInfo = path("ui" / "v1" / "model" / "withInfo") {
    get {
      complete(uiManagementService.allModelsWithLastStatus())
    }
  }

  @Path("/withInfo/{modelId}")
  @ApiOperation(value = "model", notes = "model", nickname = "model", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelInfo", response=classOf[ModelInfo]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def modelWithLastInfo = path("ui" / "v1" / "model" / "withInfo" / LongNumber) { modelId =>
    get {
      complete(uiManagementService.modelWithLastStatus(modelId))
    }
  }

  ///TODO withInfo for one

  @Path("/stopService/{modelId}")
  @ApiOperation(value = "stopService", notes = "stopService", nickname = "stopService", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Service Stopped"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deleteServices = delete {
    path("ui" / "v1" / "model" / "stopService" / LongNumber) { modelId =>
      onSuccess(uiManagementService.stopAllServices(modelId)) {
        complete(200, None)
      }
    }
  }


  @Path("/serve")
  @ApiOperation(value = "Serve Model", notes = "Serve Model", nickname = "ServeModel", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Any", required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def serveService = path("ui" / "v1" / "model" / "serve" / LongNumber) { id =>
    post {
      extractRequest { request =>
        extractRawData { bytes =>
          completeExecutionResult(
            uiManagementService.testModel(id, "/serve", bytes, request.headers
              .filter(h => TracingHeaders.isTracingHeaderName(h.name())))
          )
        }
      }
    }
  }


  @Path("/build")
  @ApiOperation(value = "Build model", notes = "Build model", nickname = "buildModel", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Model", required = true,
      dataTypeClass = classOf[BuildModelRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[ModelInfo]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def buildModel = path("ui" / "v1" / "model" / "build") {
    post {
      entity(as[BuildModelRequest]) { r =>
        complete(
          uiManagementService.buildModel(r.modelId, r.modelVersion)
        )
      }
    }
  }

  val routes = modelsWithLastInfo ~ deleteServices ~ serveService ~ buildModel ~ modelWithLastInfo

}
