package io.hydrosphere.serving.manager.controller

import javax.ws.rs.Path

import io.hydrosphere.serving.manager.service.{CreateModelVersionRequest, CreateOrUpdateModelRequest, ModelManagementService}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.api.description.ContractDescription
import io.swagger.annotations._

import scala.concurrent.duration._

case class BuildModelRequest(
  modelId: Long,
  modelVersion: Option[Long]
)

/**
  *
  */
@Path("/api/v1/model")
@Api(produces = "application/json", tags = Array("Model and Model Versions"))
class ModelController(modelManagementService: ModelManagementService)
  extends ManagerJsonSupport with ServingDataDirectives {
  implicit val timeout = Timeout(10.minutes)

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
    new ApiImplicitParam(name = "body", value = "CreateOrUpdateModelRequest", required = true,
      dataTypeClass = classOf[CreateOrUpdateModelRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addModel = path("api" / "v1" / "model") {
    post {
      entity(as[CreateOrUpdateModelRequest]) { r =>
        complete(
          modelManagementService.createModel(r)
        )
      }
    }
  }

  @Path("/")
  @ApiOperation(value = "Update model", notes = "Update model", nickname = "updateModel", httpMethod = "PUT")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "CreateOrUpdateModelRequest", required = true,
      dataTypeClass = classOf[CreateOrUpdateModelRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def updateModel = path("api" / "v1" / "model") {
    put {
      entity(as[CreateOrUpdateModelRequest]) { r =>
        complete(
          modelManagementService.updateModel(r)
        )
      }
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
      complete(modelManagementService.modelBuildsByModelId(s))
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
          modelManagementService.lastModelBuildsByModelId(s, maximum)
        )
      }
    }
  }

  @Path("/build")
  @ApiOperation(value = "Build model", notes = "Build model", nickname = "buildModel", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Model", required = true,
      dataTypeClass = classOf[BuildModelRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[ModelVersion]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def buildModel = path("api" / "v1" / "model" / "build") {
    post {
      entity(as[BuildModelRequest]) { r =>
        complete(
          modelManagementService.buildModel(r.modelId, r.modelVersion)
        )
      }
    }
  }

  @Path("version/{modelId}/last")
  @ApiOperation(value = "lastModelVersions", notes = "lastModelVersions", nickname = "lastModelVersions", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId"),
    new ApiImplicitParam(name = "maximum", required = false, dataType = "int", paramType = "query", value = "maximum", defaultValue = "10")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelVersion", response = classOf[ModelVersion], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def lastModelVersions = get {
    path("api" / "v1" / "model" / "version" / LongNumber / "last") { s =>
      parameters('maximum.as[Int]) { (maximum) =>
        complete(
          modelManagementService.lastModelVersionByModelId(s, maximum)
        )
      }
    }
  }

  @Path("version")
  @ApiOperation(value = "Add ModelVersion", notes = "Add ModelVersion", nickname = "addModelVersion", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "ModelVersion", required = true,
      dataType = "io.hydrosphere.serving.manager.service.CreateModelVersionRequest", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelVersion", response = classOf[ModelVersion]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def addModelVersion = path("api" / "v1" / "model" / "version") {
    post {
      entity(as[CreateModelVersionRequest]) { r =>
        complete(
          modelManagementService.addModelVersion(r)
        )
      }
    }
  }

  @Path("version")
  @ApiOperation(value = "All ModelVersion", notes = "All ModelVersion", nickname = "allModelVersions", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelVersion", response = classOf[ModelVersion], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def allModelVersions = path("api" / "v1" / "model" / "version") {
    get {
      complete(
        modelManagementService.allModelVersion()
      )
    }
  }

  @Path("/generate/{modelId}/{signature}")
  @ApiOperation(value = "Generate payload for model", notes = "Generate payload for model", nickname = "Generate payload for model", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId"),
    new ApiImplicitParam(name = "signature", required = true, dataType = "string", paramType = "path", value = "signature")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Seq[Any]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def generatePayloadByModelId = path("api" / "v1" / "model" / "generate" / LongNumber / Segment) { (modelName, signature) =>
    get {
      complete {
        modelManagementService.generateModelPayload(modelName, signature)
      }
    }
  }

  @Path("{modelId}/contract/text")
  @ApiOperation(value = "Submit a new contract for a model", notes = "Submit a new contract for a model", nickname = "Submit a new contract for a model", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId"),
    new ApiImplicitParam(name = "body", value = "ModelContract text message", required = true, dataTypeClass = classOf[String], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def submitTextContract = path("api" / "v1" / "model" / LongNumber / "contract" / "text") { modelId =>
    post {
      entity(as[String]) { prototext =>
        complete {
          modelManagementService.submitContract(modelId, prototext)
        }
      }
    }
  }

  @Path("{modelId}/contract/binary")
  @ApiOperation(value = "Submit a new binary contract for a model", notes = "Submit a new binary contract for a model", nickname = "Submit a new binary contract for a model", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId"),
    new ApiImplicitParam(name = "body", value = "ModelContract binary message", required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def submitBinaryContract = path("api" / "v1" / "model" / LongNumber / "contract" / "binary") { modelId =>
    post {
      extractRequest { request =>
        extractRawData { bytes =>
          complete {
            modelManagementService.submitBinaryContract(modelId, bytes)
          }
        }
      }
    }
  }

  @Path("{modelId}/contract/flat")
  @ApiOperation(value = "Submit a new flat contract for a model", notes = "Submit a flat new contract for a model", nickname = "Submit a new flat contract for a model", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId"),
    new ApiImplicitParam(name = "body", value = "ContractDescription", required = true, dataTypeClass = classOf[ContractDescription], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def submitFlatContract = path("api" / "v1" / "model" / LongNumber / "contract" / "flat") { modelId =>
    post {
      entity(as[ContractDescription]) { contractDescription =>
        complete {
          modelManagementService.submitFlatContract(modelId, contractDescription)
        }
      }
    }
  }

  @Path("version/generate/{versionId}/{signatureName}")
  @ApiOperation(value = "Generate payload for model version", notes = "Generate payload for model version", nickname = "Generate payload for model version", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "versionId", required = true, dataType = "string", paramType = "path", value = "versionId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Any", response = classOf[Seq[Any]]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def generateInputsForVersion = path("api" / "v1" / "model" / "version" / "generate" / LongNumber / Segment) { (versionId, signature) =>
    get {
      complete(
        modelManagementService.generateInputsForVersion(versionId, signature)
      )
    }
  }


  val routes: Route = listModels ~ updateModel ~ addModel ~ buildModel ~ listModelBuildsByModel ~ lastModelBuilds ~
    generatePayloadByModelId ~ submitTextContract ~ submitBinaryContract ~ submitFlatContract ~ generateInputsForVersion ~
    lastModelVersions ~ addModelVersion ~ allModelVersions
}