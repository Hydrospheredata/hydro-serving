package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.util.Timeout
import io.hydrosphere.serving.controller.{ServingDataDirectives, TracingHeaders}
import io.hydrosphere.serving.model.{ModelService, WeightedService}
import io.hydrosphere.serving.manager.service.{ServeRequest, ServingManagementService, WeightedKey, WeightedServiceCreateOrUpdateRequest}
import io.swagger.annotations._

import scala.concurrent.duration._

case class AddWeightedServiceSourceRequest(
  runtimeId: Long,
  configParams: Option[Map[String, String]]
)

/**
  *
  */
@Path("/api/v1/weightedServices")
@Api(produces = "application/json", tags = Array("Deployment: Weighted Service"))
class WeightedServiceController(servingManagementService: ServingManagementService)
  extends ManagerJsonSupport
  with ServingDataDirectives {

  implicit val timeout = Timeout(5.minutes)

  @Path("/")
  @ApiOperation(value = "weightedServices", notes = "weightedServices", nickname = "weightedServices", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "WeightedService", response = classOf[WeightedService], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listAll = path("api" / "v1" / "weightedServices") {
    get {
      complete(servingManagementService.allWeightedServices())
    }
  }
  

  @Path("/")
  @ApiOperation(value = "Add WeightedService", notes = "Add WeightedService", nickname = "addWeightedService", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "WeightedService", required = true,
      dataTypeClass = classOf[WeightedServiceCreateOrUpdateRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "WeightedService", response = classOf[WeightedService]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def create = path("api" / "v1" / "weightedServices") {
    post {
      entity(as[WeightedServiceCreateOrUpdateRequest]) { r =>
        complete(
          servingManagementService.createWeightedServices(r)
        )
      }
    }
  }

  @Path("/")
  @ApiOperation(value = "Update WeightedService", notes = "Update WeightedService", nickname = "updateWeightedService", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "WeightedServiceCreateOrUpdateRequest", required = true,
      dataTypeClass = classOf[WeightedServiceCreateOrUpdateRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "WeightedService", response = classOf[WeightedService]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def update = path("api" / "v1" / "weightedServices") {
    put {
      entity(as[WeightedServiceCreateOrUpdateRequest]) { r =>
        complete(
          servingManagementService.updateWeightedServices(r)
        )
      }
    }
  }

  @Path("/{serviceId}")
  @ApiOperation(value = "deleteWeightedService", notes = "deleteWeightedService", nickname = "deleteWeightedService", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "serviceId", required = true, dataType = "long", paramType = "path", value = "serviceId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "WeightedService Deleted"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deleteWeightedService = delete {
    path("api" / "v1" / "weightedServices" / LongNumber) { serviceId =>
      onSuccess(servingManagementService.deleteWeightedService(serviceId)) {
        complete(200, None)
      }
    }
  }

  @Path("/serve")
  @ApiOperation(value = "Serve WeightedService", notes = "Serve WeightedService", nickname = "ServeWeightedService", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Any", required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def serveService = path("api" / "v1" / "weightedServices" / "serve" / LongNumber) { id =>
    post {
      extractRequest { request =>
        extractRawData { bytes =>
          val serveRequest = ServeRequest(
            serviceKey = WeightedKey(id),
            servePath = "/serve",
            headers = request.headers.filter(h => TracingHeaders.isTracingHeaderName(h.name())),
            inputData = bytes
          )
          completeExecutionResult(servingManagementService.serve(serveRequest))
        }
      }
    }
  }

  val routes =
    listAll ~
      create ~
      update ~
      deleteWeightedService ~
      serveService

}
