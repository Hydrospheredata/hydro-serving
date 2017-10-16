package io.hydrosphere.serving.manager.controller.ui

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives.{complete, get, path, _}
import akka.util.Timeout
import io.hydrosphere.serving.manager.service.{UIManagementService, UIWeightedServiceCreateOrUpdateRequest, WeightedServiceDetails}
import io.swagger.annotations._

import scala.concurrent.duration._

@Path("/ui/v1/weightedServices")
@Api(produces = "application/json", tags = Array("UI: Weighted Services"))
class UISpecificWeightServiceController(
  uiManagementService: UIManagementService
) extends UIJsonSupport {
  implicit val timeout = Timeout(5.seconds)

  @Path("/")
  @ApiOperation(value = "weightedServices", notes = "weightedServices", nickname = "weightedServices", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "WeightedService", response = classOf[WeightedServiceDetails], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listAll = path("ui" / "v1" / "weightedServices") {
    get {
      complete(uiManagementService.allWeightedServicesDetails())
    }
  }

  @Path("/")
  @ApiOperation(value = "Add WeightedService", notes = "Add WeightedService", nickname = "addWeightedService", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "WeightedService", required = true,
      dataTypeClass = classOf[UIWeightedServiceCreateOrUpdateRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "WeightedService", response = classOf[WeightedServiceDetails]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def create = path("ui" / "v1" / "weightedServices") {
    post {
      entity(as[UIWeightedServiceCreateOrUpdateRequest]) { r =>
        complete(
          uiManagementService.createWeightedService(r)
        )
      }
    }
  }

  @Path("/")
  @ApiOperation(value = "Update WeightedService", notes = "Update WeightedService", nickname = "updateWeightedService", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "WeightedServiceCreateOrUpdateRequest", required = true,
      dataTypeClass = classOf[UIWeightedServiceCreateOrUpdateRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "WeightedService", response = classOf[WeightedServiceDetails]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def update = path("ui" / "v1" / "weightedServices") {
    put {
      entity(as[UIWeightedServiceCreateOrUpdateRequest]) { r =>
        complete(
          uiManagementService.updateWeightedService(r)
        )
      }
    }
  }

  val routes = listAll ~ update ~ create

}
