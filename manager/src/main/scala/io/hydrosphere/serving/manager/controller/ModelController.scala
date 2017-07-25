package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.service.ModelManagementService
import io.swagger.annotations._

import scala.concurrent.duration._

/**
  *
  */
@Path("/api/v1/model")
@Api(value = "/api/v1/model", produces = "application/json")
class ModelController(modelManagementService: ModelManagementService) extends ManagerJsonSupport {
  implicit val timeout = Timeout(5.minutes)

  @Path("/")
  @ApiOperation(value = "listModels", notes = "listModels", nickname = "listModels", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[Model], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listModels = path("api" / "v1" / "model") {
    get {
      complete(modelManagementService.allModels())
    }
  }

  @Path("/")
  @ApiOperation(value = "Add model", notes = "Add model", nickname = "addModel", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Model", required = true,
      dataType = "io.hydrosphere.serving.manager.model.Model", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addModel = path("api" / "v1" / "model") {
    post {
      entity(as[Model]) { r =>
        complete(
          modelManagementService.createModel(r)
        )
      }
    }
  }

  @Path("/")
  @ApiOperation(value = "Update model", notes = "Update model", nickname = "updateModel", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Model", required = true,
      dataType = "io.hydrosphere.serving.manager.model.Model", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def updateModel = path("api" / "v1" / "model") {
    put {
      entity(as[Model]) { r =>
        complete(
          modelManagementService.updateModel(r)
        )
      }
    }
  }

  @Path("/builds")
  @ApiOperation(value = "listModelBuilds", notes = "listModelBuilds", nickname = "listModelBuilds", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelBuild", response = classOf[ModelBuild], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listModelBuilds = path("api" / "v1" / "model" / "builds") {
    get {
      complete(modelManagementService.allModelBuilds())
    }
  }

  @Path("/builds/{modelId}")
  @ApiOperation(value = "listModelBuildsByModel", notes = "listModelBuildsByModel", nickname = "listModelBuildsByModel", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelBuild", response = classOf[ModelBuild], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listModelBuildsByModel = get {
    path("api" / "v1" / "model" / "builds" / LongNumber) { s =>
      complete(modelManagementService.modelBuildsByModel(s))
    }
  }

  @Path("/builds/{modelId}/last")
  @ApiOperation(value = "lastModelBuildsByModel", notes = "lastModelBuildsByModel", nickname = "lastModelBuildsByModel", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId"),
    new ApiImplicitParam(name = "maximum", required = false, dataType = "int", paramType = "query", value = "maximum", defaultValue = "10")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelBuild", response = classOf[ModelBuild], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def lastModelBuilds = get {
    path("api" / "v1" / "model" / "builds" / LongNumber / "last") { s =>
      parameters('maximum.as[Int]) { (maximum) =>
        complete(
          modelManagementService.lastModelBuildsByModel(s, maximum)
        )
      }
    }
  }

  @Path("/build")
  @ApiOperation(value = "Build model", notes = "Build model", nickname = "buildModel", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Model", required = true,
      dataType = "io.hydrosphere.serving.manager.controller.BuildModelRequest", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def buildModel = path("api" / "v1" / "model" / "build") {
    put {
      entity(as[BuildModelRequest]) { r =>
        complete(
          modelManagementService.buildModel(r.modelId, r.modelVersion)
        )
      }
    }
  }

  val routes: Route = listModels ~ updateModel ~ addModel ~ buildModel ~ listModelBuilds ~ listModelBuildsByModel ~ lastModelBuilds
}