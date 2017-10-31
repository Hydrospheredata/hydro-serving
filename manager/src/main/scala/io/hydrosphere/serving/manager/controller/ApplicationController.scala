package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.util.Timeout
import io.hydrosphere.serving.controller.TracingHeaders
import io.hydrosphere.serving.model.{ModelService, Application}
import io.hydrosphere.serving.manager.service.{ServingManagementService, ApplicationCreateOrUpdateRequest}
import io.swagger.annotations._

import scala.concurrent.duration._

case class AddApplicationSourceRequest(
  runtimeId: Long,
  configParams: Option[Map[String, String]]
)

/**
  *
  */
@Path("/api/v1/applications")
@Api(produces = "application/json", tags = Array("Deployment: Weighted Service"))
class ApplicationController(servingManagementService: ServingManagementService) extends ManagerJsonSupport {
  implicit val timeout = Timeout(5.minutes)

  @Path("/")
  @ApiOperation(value = "applications", notes = "applications", nickname = "applications", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[Application], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listAll = path("api" / "v1" / "applications") {
    get {
      complete(servingManagementService.allApplications())
    }
  }
  

  @Path("/")
  @ApiOperation(value = "Add Application", notes = "Add Application", nickname = "addApplication", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Application", required = true,
      dataTypeClass = classOf[ApplicationCreateOrUpdateRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[Application]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def create = path("api" / "v1" / "applications") {
    post {
      entity(as[ApplicationCreateOrUpdateRequest]) { r =>
        complete(
          servingManagementService.createApplications(r)
        )
      }
    }
  }

  @Path("/")
  @ApiOperation(value = "Update Application", notes = "Update Application", nickname = "updateApplication", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "ApplicationCreateOrUpdateRequest", required = true,
      dataTypeClass = classOf[ApplicationCreateOrUpdateRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[Application]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def update = path("api" / "v1" / "applications") {
    put {
      entity(as[ApplicationCreateOrUpdateRequest]) { r =>
        complete(
          servingManagementService.updateApplications(r)
        )
      }
    }
  }

  @Path("/{serviceId}")
  @ApiOperation(value = "deleteApplication", notes = "deleteApplication", nickname = "deleteApplication", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceId", required = true, dataType = "long", paramType = "path", value = "serviceId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application Deleted"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deleteApplication = delete {
    path("api" / "v1" / "applications" / LongNumber) { serviceId =>
      onSuccess(servingManagementService.deleteApplication(serviceId)) {
        complete(200, None)
      }
    }
  }

  @Path("/serve")
  @ApiOperation(value = "Serve Application", notes = "Serve Application", nickname = "ServeApplication", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Any", dataTypeClass = classOf[ServeData], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[ServeData]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def serveService = path("api" / "v1" / "applications" / "serve") {
    post {
      extractRequest { request =>
        entity(as[ServeData]) { r =>
          complete(
            servingManagementService.serveApplication(r.id, r.path.getOrElse("/serve"), r.data, request.headers
              .filter(h => TracingHeaders.isTracingHeaderName(h.name())))
          )
        }
      }
    }
  }

  val routes =
    listAll ~
      create ~
      update ~
      deleteApplication ~
      serveService

}
