package io.hydrosphere.serving.manager.controller.ui

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives.{complete, get, path, _}
import akka.util.Timeout
import io.hydrosphere.serving.manager.service.{UIRuntimeInfo, UIManagementService}
import io.swagger.annotations._

import scala.concurrent.duration._

@Path("/ui/v1/modelRuntime")
@Api(produces = "application/json", tags = Array("UI: Runtime"))
class UISpecificRuntimeController(
  uiManagementService: UIManagementService
) extends UIJsonSupport {
  implicit val timeout = Timeout(5.seconds)


  @Path("/withInfo/{modelId}")
  @ApiOperation(value = "runtimes", notes = "runtimes", nickname = "runtimes", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "RuntimeInfo", response = classOf[UIRuntimeInfo], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def runtimesForModel = path("ui" / "v1" / "modelRuntime" / "withInfo" / LongNumber) { modelId =>
    get {
      complete(uiManagementService.modelRuntimes(modelId))
    }
  }

  val routes = runtimesForModel

}
