package io.hydrosphere.serving.manager.controller.model

import java.nio.file.{Files, OpenOption, StandardOpenOption}
import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpResponse, Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.contract.utils.description.ContractDescription
import io.hydrosphere.serving.manager.controller.ServingDataDirectives
import io.hydrosphere.serving.manager.controller.model.UploadedEntity._
import io.hydrosphere.serving.manager.model.protocol.CompleteJsonProtocol._
import io.hydrosphere.serving.manager.model._
import io.hydrosphere.serving.manager.model.db.{Model, ModelBuild, ModelVersion}
import io.hydrosphere.serving.manager.service._
import io.swagger.annotations._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Path("/api/v1/model")
@Api(produces = "application/json", tags = Array("Model and Model Versions"))
class ModelController(modelManagementService: ModelManagementService)
  (implicit system: ActorSystem,  materializer: ActorMaterializer, executionContext: ExecutionContext)
  extends ServingDataDirectives {
  implicit val timeout = Timeout(10.minutes)

  @Path("/")
  @ApiOperation(value = "listModels", notes = "listModels", nickname = "listModels", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[AggregatedModelInfo], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listModels = path("api" / "v1" / "model") {
    get {
      complete(modelManagementService.allModelsAggregatedInfo())
    }
  }

  @Path("/{modelId}")
  @ApiOperation(value = "getModel", notes = "getModel", nickname = "getModel", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[AggregatedModelInfo]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getModel = path("api" / "v1" / "model" / LongNumber) { id =>
    get {
      complete(modelManagementService.getModelAggregatedInfo(id))
    }
  }

  @Path("/")
  @ApiOperation(value = "Upload model", notes = "Upload model", nickname = "uploadModel", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "CreateOrUpdateModelRequest", required = true,
      dataTypeClass = classOf[CreateOrUpdateModelRequest], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def uploadModel = path("api" / "v1" / "model") {
    post {
      entity(as[Multipart.FormData]) { (formdata: Multipart.FormData) ⇒
        val fileNamesFuture = formdata.parts.flatMapConcat { p ⇒
          logger.debug(s"Got part. Name: ${p.name} Filename: ${p.filename}")
          p.name match {
            case "model_type" if p.filename.isEmpty =>
              p.entity.dataBytes
                .map(_.decodeString("UTF-8"))
                .filterNot(_.isEmpty)
                .map(r => UploadType(modelType = r))

            case "target_source" if p.filename.isEmpty =>
              p.entity.dataBytes
                .map(_.decodeString("UTF-8"))
                .filterNot(_.isEmpty)
                .map(r => TargetSource(source = r))

            case "model_contract" if p.filename.isEmpty =>
              p.entity.dataBytes
                .map(b => ModelContract.parseFrom(b.toByteBuffer.array()))
                .map(p => Contract(modelContract = p))

            case "model_description" if p.filename.isEmpty =>
              p.entity.dataBytes
                .map(_.decodeString("UTF-8"))
                .filterNot(_.isEmpty)
                .map(r => Description(description = r))

            case "model_name" if p.filename.isEmpty =>
              p.entity.dataBytes
                .map(_.decodeString("UTF-8"))
                .filterNot(_.isEmpty)
                .map(r => ModelName(modelName = r))

            case "payload" if p.filename.isDefined =>
              val filename = p.filename.get
              val tempPath = Files.createTempFile("upload", filename)
              p.entity.dataBytes
                .map { fileBytes =>
                  Files.write(tempPath, fileBytes.toArray, StandardOpenOption.APPEND)
                  Tarball(path = tempPath)
                }
            case _ =>
              logger.warn(s"Unknown part. Name: ${p.name} Filename: ${p.filename}")
              p.entity.dataBytes.map { _ =>
                UnknownPart(p.name)
              }
          }
        }
        val dicts = fileNamesFuture.runFold(Map.empty[String, UploadedEntity]) {
          case (a, b) => a + (b.name -> b)
        }
        def uploadModel(): Future[Option[Model]] = {
          dicts.map(ModelUpload.fromMap).flatMap {
            case Some(upload) => modelManagementService.uploadModelTarball(upload)
            case None => Future.successful(None)
          }
        }

        onSuccess(uploadModel()) {
          case None => complete(400, "Incorrect input data")
          case Some(m) => complete(200, m)
        }
      }
    }
  }

  // TODO implement
  // TODO tests
  @Path("/")
  @ApiOperation(value = "Add model", notes = "Add model", nickname = "uploadModel", httpMethod = "POST")
  def addModel = path("api"/ "v1" / "model") {
    post {
      ???
    }
  }

  // TODO tests
  @Path("/index")
  @ApiOperation(value = "Index models", notes = "Index model", nickname = "indexModels", httpMethod = "POST")
  def indexModels = path("api"/ "v1" / "model" / "index") {
    post {
      entity(as[Set[Long]]) { ids =>
        complete(
          modelManagementService.indexModels(ids)
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
    new ApiImplicitParam(name = "maximum", required = false, dataType = "integer", paramType = "query", value = "maximum", defaultValue = "10")
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
          modelManagementService.buildModel(r.modelId, r.flatContract)
        )
      }
    }
  }

  @Path("version/{modelId}/last")
  @ApiOperation(value = "lastModelVersions", notes = "lastModelVersions", nickname = "lastModelVersions", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId"),
    new ApiImplicitParam(name = "maximum", required = false, dataType = "integer", paramType = "query", value = "maximum", defaultValue = "10")
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
      dataTypeClass = classOf[CreateModelVersionRequest], paramType = "body")
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
    new ApiResponse(code = 200, message = "Any"),
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
    new ApiImplicitParam(name = "body", value = "ModelContract binary message", required = true, paramType = "body", dataType = "binary")
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
    new ApiResponse(code = 200, message = "Any"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def generateInputsForVersion = path("api" / "v1" / "model" / "version" / "generate" / LongNumber / Segment) { (versionId, signature) =>
    get {
      complete(
        modelManagementService.generateInputsForVersion(versionId, signature)
      )
    }
  }

  @Path("{modelId}/flatContract")
  @ApiOperation(value = "Get flatten contract", notes = "Get flatten contract", nickname = "Get flatten contract", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ContractDescription", response = classOf[ContractDescription]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def modelContractDescription = path("api" / "v1" / "model" / LongNumber / "flatContract" ) { (modelId) =>
    get {
      complete {
        modelManagementService.modelContractDescription(modelId)
      }
    }
  }

  @Path("/version/{versionId}/flatContract")
  @ApiOperation(value = "Get flatten contract", notes = "Get flatten contract", nickname = "Get flatten contract", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "versionId", required = true, dataType = "long", paramType = "path", value = "versionId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ContractDescription", response = classOf[ContractDescription]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def versionContractDescription = path("api" / "v1" / "model" / "version" / LongNumber / "flatContract" ) { (versionId) =>
    get {
      complete {
        modelManagementService.versionContractDescription(versionId)
      }
    }
  }

  val routes: Route = listModels ~ getModel ~ updateModel ~ uploadModel ~ buildModel ~ listModelBuildsByModel ~ lastModelBuilds ~
    generatePayloadByModelId ~ submitTextContract ~ submitBinaryContract ~ submitFlatContract ~ generateInputsForVersion ~
    lastModelVersions ~ addModelVersion ~ allModelVersions ~ modelContractDescription ~ versionContractDescription
}