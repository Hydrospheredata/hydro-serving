package io.hydrosphere.serving.manager.controller.ui

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.util.Timeout
import io.hydrosphere.serving.manager.controller.ManagerJsonSupport
import io.hydrosphere.serving.manager.service.{ModelInfo, ModelManagementService}
import io.swagger.annotations.{Api, ApiOperation, ApiResponse, ApiResponses}

import scala.concurrent.duration._

@Path("/ui/v1/model")
@Api(produces = "application/json", tags = Array("UI: Model"))
class UISpecificController(
  modelManagementService: ModelManagementService
) extends ManagerJsonSupport {
  implicit val timeout = Timeout(5.seconds)

  @Path("/withInfo")
  @ApiOperation(value = "models", notes = "models", nickname = "models", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelInfo", response = classOf[ModelInfo], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def modelsWithLastInfo = path("ui" / "v1" / "model" / "withInfo") {
    get {
      complete(modelManagementService.allModelsWithLastStatus())
    }
  }

  val routes = modelsWithLastInfo

}
