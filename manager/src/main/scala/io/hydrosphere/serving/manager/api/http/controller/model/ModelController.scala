package io.hydrosphere.serving.manager.api.http.controller.model

import java.nio.file.{Files, StandardOpenOption}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cats.data.EitherT
import cats.implicits._
import io.hydrosphere.serving.contract.model_contract.ModelContract
import io.hydrosphere.serving.manager.api.http.controller.GenericController
import io.hydrosphere.serving.manager.domain.model.{Model, ModelService}
import io.hydrosphere.serving.manager.domain.model_version.{ModelVersion, ModelVersionService, ModelVersionView}
import io.hydrosphere.serving.model.api.Result
import io.swagger.annotations._
import javax.ws.rs.Path

import scala.concurrent.Future
import scala.concurrent.duration._

@Path("/api/v2/model")
@Api(produces = "application/json", tags = Array("Model and Model Versions"))
class ModelController(
  modelManagementService: ModelService,
  modelVersionManagementService: ModelVersionService
)(
  implicit val system: ActorSystem,
  val materializer: ActorMaterializer,
)
  extends GenericController {
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(10.minutes)

  @Path("/")
  @ApiOperation(value = "listModels", notes = "listModels", nickname = "listModels", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[Model], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def listModels = path("model") {
    get {
      complete(modelManagementService.allModels())
    }
  }

  @Path("/{modelId}")
  @ApiOperation(value = "getModel", notes = "getModel", nickname = "getModel", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def getModel = pathPrefix("model" / LongNumber) { id =>
    get {
      completeFRes {
        modelManagementService.getModel(id)
      }
    }
  }

  @Path("/upload")
  @ApiOperation(value = "Upload and release a model", notes = "Send POST multipart with 'payload'-tar.gz and 'metadata'-json parts", nickname = "uploadModel", httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "ModelUploadMetadata", required = true,
      dataTypeClass = classOf[ModelUploadMetadata], paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[ModelVersion]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def uploadModel = pathPrefix("model" / "upload") {
    post {
      getFileWithMeta[ModelUploadMetadata, ModelVersion] {
        case (Some(file), Some(meta)) =>
          modelManagementService.uploadModel(file, meta)
        case (None, _) => Result.clientErrorF("Couldn't find a payload in request")
        case (_, None) => Result.clientErrorF("Couldn't find a metadata in request")
      }
    }
  }

  @Path("/version")
  @ApiOperation(value = "All ModelVersion", notes = "All ModelVersion", nickname = "allModelVersions", httpMethod = "GET")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "ModelVersion", response = classOf[ModelVersionView], responseContainer = "List"),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def allModelVersions = path("model" / "version") {
    get {
      completeF(
        modelVersionManagementService.list
      )
    }
  }

  @Path("/{modelId}")
  @ApiOperation(value = "Delete model if not in app", notes = "Fails if any version of the model is deployed", nickname = "deleteModel", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "modelId", required = true, dataType = "long", paramType = "path", value = "modelId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Model", response = classOf[Model]),
    new ApiResponse(code = 500, message = "Internal server error")
  ))
  def deleteModel = pathPrefix("model" / LongNumber) { modelId =>
    delete {
      completeFRes {
        modelManagementService.deleteModel(modelId)
      }
    }
  }


  val routes: Route = listModels ~ getModel ~ uploadModel ~ allModelVersions ~ deleteModel
}