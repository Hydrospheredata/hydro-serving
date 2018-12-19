package io.hydrosphere.serving.manager.api.http.controller.application

import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import io.hydrosphere.serving.manager.api.http.controller.GenericController
import io.hydrosphere.serving.manager.domain.application.{Application, ApplicationService}
import io.swagger.annotations._
import javax.ws.rs.Path

import scala.concurrent.duration._


@Path("/api/v2/application")
@Api(produces = "application/json", tags = Array("Application"))
class ApplicationController(
  applicationManagementService: ApplicationService
) extends GenericController {
  implicit val timeout = Timeout(5.minutes)

  @Path("/")
  @ApiOperation(value = "applications", notes = "applications", nickname = "applications", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[Application], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listAll = path("application") {
    get {
      complete(applicationManagementService.allApplications())
    }
  }


  @Path("/")
  @ApiOperation(value = "Add Application", notes = "Add Application", nickname = "addApplication", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Application", required = true,
      dataTypeClass = classOf[CreateApplicationRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[Application]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def create = path("application") {
    post {
      entity(as[CreateApplicationRequest]) { r =>
        completeFRes(
          applicationManagementService.createApplication(r.name, r.namespace, r.executionGraph, r.kafkaStreaming)
        )
      }
    }
  }

  @Path("/")
  @ApiOperation(value = "Update Application", notes = "Update Application", nickname = "updateApplication", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "ApplicationCreateOrUpdateRequest", required = true,
      dataTypeClass = classOf[UpdateApplicationRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[Application]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def update = pathPrefix("application") {
    put {
      entity(as[UpdateApplicationRequest]) { r =>
        completeFRes(
          applicationManagementService.updateApplication(r.id, r.name, r.namespace, r.executionGraph, r.kafkaStreaming.getOrElse(List.empty))
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
    pathPrefix("application" / LongNumber) { serviceId =>
      completeFRes(applicationManagementService.deleteApplication(serviceId))
    }
  }

  @Path("/generateInputs/{appId}/{signatureName}")
  @ApiOperation(value = "Generate payload for application", notes = "Generate payload for application", nickname = "Generate payload for application", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "appId", required = true, dataType = "long", paramType = "path", value = "appId"),
    new ApiImplicitParam(name = "signatureName", required = false, dataType = "string", paramType = "path", value = "signatureName")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Seq[Any]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def generateInputsForApp = pathPrefix("application" / "generateInputs" / LongNumber / Segment) { (appId, signatureName) =>
    get {
      complete(
        applicationManagementService.generateInputsForApplication(appId, signatureName)
      )
    }
  }

  val routes =
    listAll ~
      create ~
      update ~
      deleteApplication ~
      generateInputsForApp

}