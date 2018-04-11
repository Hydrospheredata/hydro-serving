package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._
import io.hydrosphere.serving.manager.model.db.Service
import io.hydrosphere.serving.manager.service._
import io.swagger.annotations._

import scala.concurrent.duration._


@Path("/api/v1/service")
@Api(produces = "application/json", tags = Array("Service"))
class ServiceController(
  serviceManagementService: ServiceManagementService,
  applicationManagementService: ApplicationManagementService
) extends ServingDataDirectives {
  implicit val timeout = Timeout(5.minutes)

  @Path("/")
  @ApiOperation(value = "listServices", notes = "listServices", nickname = "listServices", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Service", response = classOf[Service], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listAll = path("api" / "v1" / "service") {
    get {
      complete(serviceManagementService.allServices())
    }
  }

  @Path("/fetchByModelId/{modelId}")
  @ApiOperation(value = "fetchByModelId", notes = "fetchByModelId", nickname = "fetchByModelId", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Service", response = classOf[Service], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def fetchByModelId = get {
    path("api" / "v1" / "service" / "fetchByModelId"/ Segment) { modelId =>
      complete(serviceManagementService.getServicesByModel(modelId.toLong))
    }
  }

  /*@Path("/{serviceId}")
  @ApiOperation(value = "deleteService", notes = "deleteService", nickname = "deleteService", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceId", required = true, dataType = "long", paramType = "path", value = "serviceId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Service Deleted"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deleteService = delete {
    path("api" / "v1" / "service" / LongNumber) { serviceId =>
      onSuccess(serviceManagementService.deleteService(serviceId)) {
        complete(200, None)
      }
    }
  }*/

  @Path("/{serviceId}")
  @ApiOperation(value = "getService", notes = "getService", nickname = "getService", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceId", required = true, dataType = "long", paramType = "path", value = "serviceId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Service", response = classOf[Service]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getService = get {
    path("api" / "v1" / "service" / Segment) { serviceId =>
      complete(serviceManagementService.getService(serviceId.toLong))
    }
  }

  @Path("/fetchByIds")
  @ApiOperation(value = "fetchById", notes = "fetchById", nickname = "fetchById", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "ids", required = true,
      dataTypeClass = classOf[Long], paramType = "body", collectionFormat = "List")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Service", response = classOf[Service], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def fetchByIds = path("api" / "v1" / "service" / "fetchByIds") {
    post {
      entity(as[Seq[Long]]) { r =>
        complete(
          serviceManagementService.servicesByIds(r)
        )
      }
    }
  }

  /*@Path("/")
  @ApiOperation(value = "Add Service", notes = "Add Service", nickname = "addService", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "CreateServiceRequest", required = true,
      dataTypeClass = classOf[CreateServiceRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Service", response = classOf[Service]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addService = path("api" / "v1" / "service") {
    post {
      entity(as[CreateServiceRequest]) { r =>
        complete(
          serviceManagementService.addService(r)
        )
      }
    }
  }


  @Path("/serve/{serviceId}")
  @ApiOperation(value = "Serve Service by id", notes = "Serve Service by id", nickname = "Serve Service by id", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceId", required = true, dataType = "long", paramType = "path", value = "serviceId"),
    new ApiImplicitParam(name = "body", value = "Any", dataTypeClass = classOf[List[_]], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Seq[Any]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def serveByServiceId = path("api" / "v1" / "service" / "serve" / LongNumber) { (serviceId) =>
    post {
      extractRawData { bytes =>
        completeExecutionResult(serviceManagementService.serveService(serviceId, bytes))
      }
    }
  }



  */

  val routes: Route =
    listAll ~ /*addService ~ deleteService ~*/ getService ~
      fetchByModelId ~  fetchByIds
}
