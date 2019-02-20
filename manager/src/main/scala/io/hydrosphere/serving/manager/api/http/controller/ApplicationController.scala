package io.hydrosphere.serving.manager.api.http.controller

import akka.http.scaladsl.server.Directives._
import cats.effect.Effect
import cats.syntax.functor._
import io.hydrosphere.serving.manager.domain.application._
import io.swagger.annotations._
import javax.ws.rs.Path

@Path("/api/v2/application")
@Api(produces = "application/json", tags = Array("Application"))
class ApplicationController[F[_]: Effect](
  appService: ApplicationService[F],
  appRepository: ApplicationRepository[F]
) extends AkkaHttpControllerDsl {

  @Path("/")
  @ApiOperation(value = "applications", notes = "applications", nickname = "applications", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[Application], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listAll = path("application") {
    get {
      completeF(appRepository.all())
    }
  }

  @Path("/{appName}")
  @ApiOperation(value = "getApp", notes = "getApp", nickname = "getApp", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "appName", required = true, dataType = "string", paramType = "path", value = "name")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application", response = classOf[Application], responseContainer = "List"),
    new ApiResponse(code = 404, message = "Not Found"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getApp = path("application" / Segment) { appName =>
    get {
      completeFRes(appService.get(appName))
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
          appService.create(r).map(_.right.map(_.started))
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
          appService.update(r).map(_.right.map(_.started))
        )
      }
    }
  }

  @Path("/{applicationName}")
  @ApiOperation(value = "deleteApplication", notes = "deleteApplication", nickname = "deleteApplication", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "applicationName", required = true, dataType = "string", paramType = "path", value = "name")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Application Deleted"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deleteApplicationByName = delete {
    path("application" / Segment) { appName =>
      completeFRes(appService.delete(appName))
    }
  }


  @Path("/generateInputs/{applicationName}/")
  @ApiOperation(value = "Generate payload for application", notes = "Generate payload for application", nickname = "Generate payload for application", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "applicationName", required = true, dataType = "string", paramType = "path", value = "name"),
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Seq[Any]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def generateInputsForApp = pathPrefix("application" / "generateInputs" / Segment) { appName =>
    get {
      completeFRes(
        appService.generateInputs(appName)
      )
    }
  }

  val routes =
    listAll ~
      create ~
      update ~
      deleteApplicationByName ~
      generateInputsForApp ~
      getApp
}