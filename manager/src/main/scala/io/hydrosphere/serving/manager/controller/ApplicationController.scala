package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.Application
import io.hydrosphere.serving.manager.service._
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
@Api(produces = "application/json", tags = Array("Deployment: Application"))
class ApplicationController(
  servingManagementService: ServingManagementService
) extends ManagerJsonSupport with ServingDataDirectives{
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

  @Path("/serve/{applicationName}")
  @ApiOperation(value = "Serve Application", notes = "Serve Application", nickname = "ServeApplication", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "applicationName", required = true, dataType = "string", paramType = "path", value = "applicationName"),
    new ApiImplicitParam(name = "body", value = "Any", required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Seq[Any]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def serve = path("api" / "v1" / "applications" / "serve" / Segment)  { name =>
    post {
      extractRequest { request =>
        extractRawData { bytes =>
          val serveRequest = ServeRequest(
            serviceKey = ApplicationName(name),
            servePath = "/serve",
            headers = request.headers.filter(h => TracingHeaders.isTracingHeaderName(h.name())),
            inputData = bytes
          )
          completeExecutionResult(servingManagementService.serve(serveRequest))
        }
      }
    }
  }

  @Path("/serveById/{applicationId}")
  @ApiOperation(value = "Serve Application", notes = "Serve Application", nickname = "ServeApplication", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "applicationId", required = true, dataType = "long", paramType = "path", value = "modelId"),
    new ApiImplicitParam(name = "body", value = "Any", required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Seq[Any]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def serveById = path("api" / "v1" / "applications" / "serveById" / LongNumber)  { id =>
    post {
      extractRequest { request =>
        extractRawData { bytes =>
          val serveRequest = ServeRequest(
            serviceKey = ApplicationKey(id),
            servePath = "/serve",
            headers = request.headers.filter(h => TracingHeaders.isTracingHeaderName(h.name())),
            inputData = bytes
          )
          completeExecutionResult(servingManagementService.serve(serveRequest))
        }
      }
    }
  }

  @Path("/generateInputs/{appId}")
  @ApiOperation(value = "Generate payload for application", notes = "Generate payload for application", nickname = "Generate payload for application", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "appId", required = true, dataType = "string", paramType = "path", value = "appId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Seq[Any]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def generateInputsForApp = path("api" / "v1" / "applications" / "generateInputs" / LongNumber) { appId =>
    get {
      complete(
        servingManagementService.generateInputsForApplication(appId)
      )
    }
  }

  val routes =
    listAll ~
      create ~
      update ~
      deleteApplication ~
      serve ~
      serveById ~
      generateInputsForApp

}
