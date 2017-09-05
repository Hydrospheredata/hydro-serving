package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.controller.TracingHeaders
import io.hydrosphere.serving.model.ModelService
import io.hydrosphere.serving.manager.service.{CreateModelServiceRequest, RuntimeManagementService, ServingManagementService}
import io.swagger.annotations._

import scala.concurrent.duration._

case class ServeData(
  id: Long,
  path: Option[String],
  data: Seq[Any]
)

/**
  *
  */
@Path("/api/v1/modelService")
@Api(produces = "application/json", tags = Array("Deployment: Model Service"))
class ModelServiceController(
  runtimeManagementService: RuntimeManagementService,
  servingManagementService: ServingManagementService
) extends ManagerJsonSupport {
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

  @Path("/fetchByIds")
  @ApiOperation(value = "fetchById", notes = "fetchById", nickname = "fetchById", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "ids", required = true,
      dataTypeClass = classOf[Long], paramType = "body", collectionFormat = "List")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelService", response = classOf[ModelService]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def fetchByIds = path("api" / "v1" / "modelService" / "fetchByIds") {
    post {
      entity(as[Seq[Long]]) { r =>
        complete(
          runtimeManagementService.servicesByIds(r)
        )
      }
    }
  }

  @Path("/")
  @ApiOperation(value = "Add ModelService", notes = "Add ModelService", nickname = "addModelService", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "CreateModelServiceRequest", required = true,
      dataTypeClass = classOf[CreateModelServiceRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelService", response = classOf[ModelService]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addService = path("api" / "v1" / "modelService") {
    post {
      entity(as[CreateModelServiceRequest]) { r =>
        complete(
          runtimeManagementService.addService(r)
        )
      }
    }
  }


  @Path("/instances/{serviceId}")
  @ApiOperation(value = "listInstances", notes = "listInstances", nickname = "listInstances", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceId", required = true, dataType = "long", paramType = "path", value = "serviceId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelService", response = classOf[ModelService], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listInstances = get {
    path("api" / "v1" / "modelService" / "instances" / Segment) { serviceId =>
      complete(runtimeManagementService.instancesForService(serviceId.toLong))
    }
  }

  @Path("/{serviceId}")
  @ApiOperation(value = "deleteService", notes = "deleteService", nickname = "deleteService", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceId", required = true, dataType = "long", paramType = "path", value = "serviceId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Service Deleted"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deleteService = delete {
    path("api" / "v1" / "modelService" / LongNumber) { serviceId =>
      onSuccess(runtimeManagementService.deleteService(serviceId)) {
        complete(200, None)
      }
    }
  }

  @Path("/{serviceId}")
  @ApiOperation(value = "getService", notes = "getService", nickname = "getService", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceId", required = true, dataType = "long", paramType = "path", value = "serviceId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelService", response = classOf[ModelService]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getService = get {
    path("api" / "v1" / "modelService" / Segment) { serviceId =>
      complete(runtimeManagementService.getService(serviceId.toLong))
    }
  }

  @Path("/serve")
  @ApiOperation(value = "Serve ModelService", notes = "Serve ModelService", nickname = "ServeModelService", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Any", dataTypeClass = classOf[ServeData], required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response=classOf[ServeData]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def serveService = path("api" / "v1" / "modelService" / "serve" ) {
    post {
      extractRequest { request =>
        entity(as[ServeData]) { r =>
          complete(
            servingManagementService.serveModelService(r.id, r.path.getOrElse("/serve"), r.data, request.headers
              .filter(h => TracingHeaders.isTracingHeaderName(h.name())))
          )
        }
      }
    }
  }

  val routes: Route = listAll ~ addService ~ listInstances ~ deleteService ~ getService ~ serveService ~ fetchByIds
}
