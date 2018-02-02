package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.{Application, ManagerJsonSupport}
import io.hydrosphere.serving.manager.service._
import io.swagger.annotations._
import spray.json.JsObject

import scala.concurrent.duration._


@Path("/api/v1/applications")
@Api(produces = "application/json", tags = Array("Application"))
class ApplicationController(
  applicationManagementService: ApplicationManagementService
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
      complete(applicationManagementService.allApplications())
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
          applicationManagementService.createApplications(r)
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
          applicationManagementService.updateApplications(r)
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
      onSuccess(applicationManagementService.deleteApplication(serviceId)) {
        complete(200, None)
      }
    }
  }

  @Path("/generateInputs/{appId}")
  @ApiOperation(value = "Generate payload for application", notes = "Generate payload for application", nickname = "Generate payload for application", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "appId", required = true, dataType = "long", paramType = "path", value = "appId"),
    new ApiImplicitParam(name = "signatureName", required = false, dataType = "string", paramType = "path", value = "signatureName")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Seq[Any]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def generateInputsForApp = path("api" / "v1" / "applications" / "generateInputs" / LongNumber / Segment) { (appId, signatureName) =>
    get {
      complete(
        applicationManagementService.generateInputsForApplication(appId, signatureName)
      )
    }
  }

  @Path("/serve/{applicationId}/{signatureName}")
  @ApiOperation(value = "Serve Application by id", notes = "Serve Application by id", nickname = "Serve Application by id", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "applicationId", required = true, dataType = "long", paramType = "path", value = "applicationId"),
    new ApiImplicitParam(name = "signatureName", required = false, dataType = "string", paramType = "path", value = "signatureName"),
    new ApiImplicitParam(name = "body", value = "Any", dataTypeClass = classOf[List[_]], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def serveById = path("api" / "v1" / "applications" / "serve" / LongNumber / Segment) { (serviceId, signatureName) =>
    post {
      entity(as[JsObject]) { bytes =>
        complete {
          applicationManagementService.serveJsonApplication(
            JsonServeRequest(
              targetId = serviceId,
              signatureName = signatureName,
              inputs = bytes
            )
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
      generateInputsForApp ~
      serveById

}
