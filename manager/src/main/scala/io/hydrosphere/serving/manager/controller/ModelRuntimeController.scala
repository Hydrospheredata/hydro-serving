package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.ModelRuntime
import io.hydrosphere.serving.manager.service.ModelManagementService
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
    new ApiResponse(code = 200, message = "ModelRuntime", response = classOf[ModelRuntime], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listModelRuntimes = path("api" / "v1" / "modelRuntime") {
    get {
      complete(modelManagementService.allModelRuntime())
    }
  }


  @Path("/")
  @ApiOperation(value = "Add ModelRuntime", notes = "Add ModelRuntime", nickname = "addModelRuntime", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "ModelRuntime", required = true,
      dataType = "io.hydrosphere.serving.manager.model.ModelRuntime", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelRuntime", response = classOf[ModelRuntime]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addModelRuntime = path("api" / "v1" / "modelRuntime") {
    post {
      entity(as[ModelRuntime]) { r =>
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
    new ApiResponse(code = 200, message = "ModelRuntime", response = classOf[ModelRuntime], responseContainer = "List"),
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


  val routes: Route = listModelRuntimes ~ addModelRuntime
}
