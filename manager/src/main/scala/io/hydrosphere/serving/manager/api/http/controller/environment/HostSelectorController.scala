package io.hydrosphere.serving.manager.api.http.controller.environment

import javax.ws.rs.Path
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.api.http.controller.GenericController
import io.hydrosphere.serving.manager.domain.host_selector.{HostSelector, HostSelectorService}
import io.hydrosphere.serving.manager.service._
import io.swagger.annotations._

import scala.concurrent.duration._


@Path("/api/v2/hostSelector")
@Api(produces = "application/json", tags = Array("Host Selectors"))
class HostSelectorController(
  hostSelectorService: HostSelectorService
) extends GenericController {
  implicit val timeout = Timeout(5.seconds)

  @Path("/")
  @ApiOperation(value = "listHostSelectors", notes = "listHostSelectors", nickname = "listHostSelectors", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Serving Hosts", response = classOf[HostSelector], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listHostSelectors = pathPrefix("hostSelector") {
    get {
      complete(hostSelectorService.all())
    }
  }

  @Path("/")
  @ApiOperation(value = "createHostSelector", notes = "createHostSelector", nickname = "createHostSelector", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Host Object", required = true,
      dataTypeClass = classOf[CreateEnvironmentRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Host", response = classOf[HostSelector]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def createHostSelector = pathPrefix("hostSelector") {
    entity(as[CreateEnvironmentRequest]) { r =>
      complete(
        hostSelectorService.create(r.name, r.placeholders)
      )
    }
  }

  @Path("/{environmentId}")
  @ApiOperation(value = "deleteHostSelector", notes = "deleteHostSelector", nickname = "deleteHostSelector", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "environmentId", required = true, dataType = "long", paramType = "path", value = "environmentId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Environment Deleted"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deleteHostSelector = delete {
    pathPrefix("hostSelector" / LongNumber) { environmentId =>
      onSuccess(hostSelectorService.delete(environmentId)) {
        complete(200, environmentId)
      }
    }
  }

  val routes: Route = listHostSelectors ~ createHostSelector ~ deleteHostSelector
}
