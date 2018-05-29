package io.hydrosphere.serving.manager.controller.application

import javax.ws.rs.Path
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import io.hydrosphere.serving.manager.controller.{GenericController, ServingDataDirectives, TracingHeaders}
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._
import io.hydrosphere.serving.manager.model.db.Application
import io.hydrosphere.serving.manager.service._
import io.hydrosphere.serving.manager.service.application.{ApplicationManagementService, JsonServeRequest, RequestTracingInfo}
import io.swagger.annotations._
import spray.json.JsObject

import scala.concurrent.duration._


@Path("/api/v1/applications")
@Api(produces = "application/json", tags = Array("Application"))
class ApplicationController(
  applicationManagementService: ApplicationManagementService
) extends GenericController with ServingDataDirectives {
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
                         dataTypeClass = classOf[CreateApplicationRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[Application]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def create = path("api" / "v1" / "applications") {
    post {
      entity(as[CreateApplicationRequest]) { r =>
        completeFRes(
          applicationManagementService.createApplication(r.name, r.executionGraph, r.kafkaStreaming)
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
  def update = path("api" / "v1" / "applications") {
    put {
      entity(as[UpdateApplicationRequest]) { r =>
        completeFRes(
          applicationManagementService.updateApplication(r.id, r.name, r.executionGraph, r.kafkaStreaming.map(_.toList).getOrElse(List.empty))
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
      //TODO simplify optionalHeaderValueByName
      optionalHeaderValueByName(TracingHeaders.xRequestId) {
        reqId => {
          optionalHeaderValueByName(TracingHeaders.xB3TraceId) {
            reqB3Id => {
              optionalHeaderValueByName(TracingHeaders.xB3SpanId) {
                reqB3SpanId => {
                  entity(as[JsObject]) { bytes =>
                    complete {
                      applicationManagementService.serveJsonApplication(
                        JsonServeRequest(
                          targetId = serviceId,
                          signatureName = signatureName,
                          inputs = bytes
                        ),
                        reqId.map(xRequestId =>
                                    RequestTracingInfo(
                                      xRequestId = xRequestId,
                                      xB3requestId = reqB3Id,
                                      xB3SpanId = reqB3SpanId
                                    )
                        )
                      )
                    }
                  }
                }
              }
            }
          }
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
