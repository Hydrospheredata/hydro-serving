package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.ModelService
import io.hydrosphere.serving.manager.service.RuntimeManagementService
import io.swagger.annotations._

import scala.concurrent.duration._

/**
  *
  */
@Path("/api/v1/modelService")
@Api(value = "/api/v1/modelService", produces = "application/json")
class ModelServiceController (runtimeManagementService: RuntimeManagementService) extends ManagerJsonSupport {
  implicit val timeout = Timeout(5.minutes)

  @Path("/")
  @ApiOperation(value = "listModelServices", notes = "listModelServices", nickname = "listModelServices", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelService", response = classOf[ModelService], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listAll = path("api" / "v1" / "modelService") {
    get {
      complete(runtimeManagementService.allServices())
    }
  }

  @Path("/")
  @ApiOperation(value = "Add ModelService", notes = "Add ModelService", nickname = "addModelService", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "ModelService", required = true,
      dataType = "io.hydrosphere.serving.manager.model.ModelService", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelService", response = classOf[ModelService]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addService = path("api" / "v1" / "modelService") {
    post {
      entity(as[ModelService]) { r =>
        complete(
          runtimeManagementService.addService(r)
        )
      }
    }
  }


  @Path("/{serviceId}")
  @ApiOperation(value = "listInstances", notes = "listInstances", nickname = "listInstances", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceId", required = true, dataType = "long", paramType = "path", value = "serviceId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelService", response = classOf[ModelService], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listInstances = get {
    path("api" / "v1" / "modelService" / "instances" / LongNumber) { serviceId=>
      complete(runtimeManagementService.instancesForService(serviceId))
    }
  }

  val routes: Route = listAll ~ addService ~ listInstances
}
