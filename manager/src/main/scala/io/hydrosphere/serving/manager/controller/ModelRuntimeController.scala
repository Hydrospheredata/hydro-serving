package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.ModelVersion
import io.hydrosphere.serving.manager.service.{CreateModelRuntime, ModelManagementService}
import io.swagger.annotations._

import scala.concurrent.duration._

/**
  *
  */
@Path("/api/v1/modelRuntime")
@Api(produces = "application/json", tags = Array("Models: Model Runtime"))
class ModelRuntimeController (modelManagementService: ModelManagementService) extends ManagerJsonSupport {
  implicit val timeout = Timeout(5.minutes)

  @Path("/")
  @ApiOperation(value = "listModelRuntimes", notes = "listModelRuntimes", nickname = "listModelRuntimes", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelRuntime", response = classOf[ModelVersion], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listModelRuntimes = path("api" / "v1" / "modelRuntime") {
    get {
      complete(modelManagementService.allModelRuntime())
    }
  }


  @Path("/byTag")
  @ApiOperation(value = "listModelRuntimesByTag", notes = "listModelRuntimesByTag", nickname = "listModelRuntimesByTag", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "tags", required = true,
      dataTypeClass = classOf[String], paramType = "body", collectionFormat = "List")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelRuntime", response = classOf[ModelVersion], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listModelRuntimesByTag = path("api" / "v1" / "modelRuntime" / "byTag") {
    post {
      entity(as[Seq[String]]) { r =>
        complete(
          modelManagementService.modelRuntimeByTag(r)
        )
      }
    }
  }


  @Path("/")
  @ApiOperation(value = "Add ModelRuntime", notes = "Add ModelRuntime", nickname = "addModelRuntime", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "ModelRuntime", required = true,
      dataType = "io.hydrosphere.serving.manager.service.CreateModelRuntime", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelRuntime", response = classOf[ModelVersion]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addModelRuntime = path("api" / "v1" / "modelRuntime") {
    post {
      entity(as[CreateModelRuntime]) { r =>
        complete(
          modelManagementService.addModelRuntime(r)
        )
      }
    }
  }

  @Path("/{modelId}/last")
  @ApiOperation(value = "lastModelRuntimes", notes = "lastModelRuntimes", nickname = "lastModelRuntimes", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId"),
    new ApiImplicitParam(name = "maximum", required = false, dataType = "int", paramType = "query", value = "maximum", defaultValue = "10")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelRuntime", response = classOf[ModelVersion], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def lastModelBuilds = get {
    path("api" / "v1" / "modelRuntime" / LongNumber / "last") { s =>
      parameters('maximum.as[Int]) { (maximum) =>
        complete(
          modelManagementService.lastModelRuntimeByModel(s, maximum)
        )
      }
    }
  }

  @Path("/generateInputs/{runtimeId}/{signatureName}")
  @ApiOperation(value = "Generate payload for model runtime", notes = "Generate payload for model runtime", nickname = "Generate payload for model runtime", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "runtimeId", required = true, dataType = "string", paramType = "path", value = "runtimeId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Seq[Any]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def generateInputsForRuntime = path("api" / "v1" / "modelRuntime" / "generateInputs" / LongNumber / Segment) { (runtimeId, signature) =>
    get {
      complete(
        modelManagementService.generateInputsForRuntime(runtimeId, signature)
      )
    }
  }


  val routes: Route = listModelRuntimes ~ addModelRuntime ~ lastModelBuilds ~ listModelRuntimesByTag ~ generateInputsForRuntime
}
