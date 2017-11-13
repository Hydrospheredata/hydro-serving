package io.hydrosphere.serving.manager.controller.ui

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives.{complete, get, path, _}
import akka.util.Timeout
import io.hydrosphere.serving.manager.service._
import io.swagger.annotations._

import scala.concurrent.duration._

@Path("/ui/v1/applications")
@Api(produces = "application/json", tags = Array("UI: Applications"))
class UISpecificWeightServiceController(
  uiManagementService: UIManagementService
) extends UIJsonSupport {
  implicit val timeout = Timeout(5.seconds)

  @Path("/")
  @ApiOperation(value = "applications", notes = "applications", nickname = "applications", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[ApplicationDetails], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listAll = path("ui" / "v1" / "applications") {
    get {
      complete(uiManagementService.allApplicationsDetails())
    }
  }

  @Path("/")
  @ApiOperation(value = "Add Application", notes = "Add Application", nickname = "addApplication", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Application", required = true,
      dataTypeClass = classOf[UIApplicationCreateOrUpdateRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[ApplicationDetails]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def create = path("ui" / "v1" / "applications") {
    post {
      entity(as[UIApplicationCreateOrUpdateRequest]) { r =>
        complete(
          uiManagementService.createApplication(r)
        )
      }
    }
  }

  @Path("/")
  @ApiOperation(value = "Update Application", notes = "Update Application", nickname = "updateApplication", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "ApplicationCreateOrUpdateRequest", required = true,
      dataTypeClass = classOf[UIApplicationCreateOrUpdateRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[ApplicationDetails]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def update = path("ui" / "v1" / "applications") {
    put {
      entity(as[UIApplicationCreateOrUpdateRequest]) { r =>
        complete(
          uiManagementService.updateApplication(r)
        )
      }
    }
  }

  val routes = listAll ~ update ~ create

}
